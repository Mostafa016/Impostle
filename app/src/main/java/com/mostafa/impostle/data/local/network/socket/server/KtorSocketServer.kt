package com.mostafa.impostle.data.local.network.socket.server

import android.util.Log
import com.mostafa.impostle.core.util.Debugging.TAG
import com.mostafa.impostle.data.local.network.WifiHelper
import com.mostafa.impostle.data.local.network.socket.ConnectionEvent
import com.mostafa.impostle.data.local.network.socket.MessageEvent
import com.mostafa.impostle.data.util.KtorSocketUtil.readPacket
import com.mostafa.impostle.data.util.KtorSocketUtil.writePacket
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.Connection
import io.ktor.network.sockets.ServerSocket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.isClosed
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.network.sockets.toJavaAddress
import io.ktor.util.network.port
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import java.net.SocketException
import java.nio.channels.ClosedChannelException
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

class KtorSocketServer
    @Inject
    constructor(
        private val wifiHelper: WifiHelper,
    ) : SocketServer {
        private val _listeningState = MutableStateFlow<ServerListeningState>(ServerListeningState.Idle)
        override val listeningState: StateFlow<ServerListeningState> = _listeningState.asStateFlow()

        private val _connectionEvents =
            MutableSharedFlow<ConnectionEvent>(replay = 16, extraBufferCapacity = EVENT_BUFFER_CAPACITY)
        override val connectionEvents = _connectionEvents.asSharedFlow()

        private val _messageEvents =
            MutableSharedFlow<MessageEvent>(replay = 16, extraBufferCapacity = EVENT_BUFFER_CAPACITY)
        override val messageEvents = _messageEvents.asSharedFlow()

        private var socketServer: ServerSocket? = null
        private var serverPort: Int? = null
        private val clientConnections =
            ConcurrentHashMap<String, Connection>() // ConcurrentHashMap<ClientID, Ktor Connection>
        private val clientJobs = ConcurrentHashMap<String, Job>() // To manage read jobs

        override suspend fun startListening() {
            val currentListeningState = _listeningState.value
            if (currentListeningState is ServerListeningState.Listening) {
                Log.w(TAG, "Server already listening.")
                return
            }
            try {
                setupServer()
                launchServerAcceptLoop()
            } catch (e: CancellationException) {
                Log.i(TAG, "Server listening coroutine cancelled by parent")
                throw e
            } catch (e: SocketException) {
                _listeningState.value =
                    ServerListeningState.Error("Failed to assign socket to server: ${e.message}")
                Log.e(TAG, "Failed to assign socket to server.", e)
            } catch (e: NullPointerException) {
                Log.e(TAG, "Couldn't get IP Address of device. Check Wi-Fi connection.", e)
                _listeningState.value =
                    ServerListeningState.Error("Couldn't get IP of device: ${e.message}")
            } catch (e: ClosedChannelException) {
                Log.i(TAG, "KtorSocketServer: ServerSocket is closed")
            } catch (e: Exception) {
                Log.e(TAG, "Something went wrong in the server accept loop.", e)
                _listeningState.value =
                    ServerListeningState.Error("Something went wrong in the server: ${e.message}")
            } finally {
                cleanupResources()
            }
        }

        override fun stopListening() {
            cleanupResources()
            _listeningState.value = ServerListeningState.Idle
            Log.i(TAG, "Server Stopped listening.")
        }

        override suspend fun sendToClient(
            clientId: String,
            data: String,
        ): Boolean {
            try {
                val clientSendChannel = clientConnections[clientId]!!.output
                clientSendChannel.writePacket(data)
                _messageEvents.tryEmit(MessageEvent.Sent(clientId, data))
                return true
            } catch (e: NullPointerException) {
                Log.e(
                    TAG,
                    "Client ($clientId) is not connected or clientConnections state is inconsistent.",
                )
                return false
            }
        }

        override suspend fun sendToAll(data: String): Boolean {
            clientConnections.keys.forEach { clientId ->
                val failure = !sendToClient(clientId, data)
                if (failure) {
                    Log.e(
                        TAG,
                        "Failed to send to clientId $clientId, skipped sending to rest of clients",
                    )
                    return false
                }
            }
            return true
        }

        override fun disconnectClient(clientId: String) {
            cleanupClient(clientId)
        }

        //region startListening() Helpers
        private fun setupServer() {
            val selectorManager = SelectorManager(Dispatchers.IO)
            val serverIP = wifiHelper.ipAddress
            socketServer =
                aSocket(selectorManager)
                    .tcp()
                    .bind(serverIP!!, 0) {
                        reuseAddress = true
                    }.also {
                        serverPort = it.localAddress.toJavaAddress().port
                        _listeningState.value = ServerListeningState.Listening(serverPort!!)
                        Log.d(TAG, "Server is listening at ${it.localAddress}")
                    }
        }

        private suspend fun launchServerAcceptLoop() =
            supervisorScope {
                while (socketServer?.isClosed == false) {
                    val clientSocket = socketServer!!.accept()
                    Log.d(TAG, "Server accepted connection: ${clientSocket.remoteAddress}")

                    val clientId = clientSocket.remoteAddress.toString() // Or a better unique ID

                    // Prevent duplicate connections from same ID if cleanup was slow
                    if (clientConnections.containsKey(clientId)) {
                        Log.e(TAG, "Client ($clientId) already connected to server. Closing.")
                        clientSocket.close()
                        continue
                    }

                    val connection =
                        Connection(
                            clientSocket,
                            clientSocket.openReadChannel(),
                            clientSocket.openWriteChannel(autoFlush = true),
                        )
                    clientConnections[clientId] = connection

                    // Launch a dedicated reader job for this client
                    _connectionEvents.emit(ConnectionEvent.Connected(clientId))
                    val clientJob = launch { launchClientReadLoop(clientId, connection) }
                    clientJobs[clientId] = clientJob
                }
            }

        private suspend fun launchClientReadLoop(
            clientId: String,
            connection: Connection,
        ) {
            try {
                while (!connection.socket.isClosed) {
                    val line = connection.input.readPacket()
                    if (line == null) {
                        Log.w(TAG, "Client $clientId disconnected (read null).")
                        break // Exit loop on clean disconnect
                    }
                    Log.d(TAG, "Server received from $clientId: $line")
                    _messageEvents.emit(MessageEvent.Received(clientId, line))
                }
            } catch (e: CancellationException) {
                Log.i(TAG, "Client $clientId read loop cancelled.")
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Client $clientId read loop error: ${e.message}", e)
            } finally {
                Log.d(TAG, "Client $clientId read loop finished.")
                cleanupClient(clientId)
            }
        }
        //endregion

        //region General Helpers
        private fun cleanupClient(clientId: String) {
            clientConnections.remove(clientId)?.also { connection ->
                connection.socket.close()
                clientJobs.remove(clientId)?.cancel()
                _connectionEvents.tryEmit(ConnectionEvent.Disconnected(clientId))
                Log.i(TAG, "Cleaned up client: $clientId")
            }
        }

        private fun cleanupAllClients() {
            Log.d(TAG, "Cleaning up all clients...")
            clientJobs.keys.toList().forEach { cleanupClient(it) }
            Log.d(TAG, "Cleaned up all clients.")
        }

        private fun cleanupResources() {
            cleanupAllClients()
            if (socketServer?.isClosed == false) socketServer?.close()
            socketServer = null
            serverPort = null
        }

        //endregion
        private companion object {
            const val EVENT_BUFFER_CAPACITY = 64
        }
    }
