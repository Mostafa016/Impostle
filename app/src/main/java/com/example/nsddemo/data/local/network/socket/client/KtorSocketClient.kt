package com.example.nsddemo.data.local.network.socket.client

import android.util.Log
import com.example.nsddemo.core.util.Debugging.TAG
import com.example.nsddemo.data.local.network.socket.ConnectionEvent
import com.example.nsddemo.data.local.network.socket.MessageEvent
import com.example.nsddemo.data.util.KtorSocketUtil.readPacket
import com.example.nsddemo.data.util.KtorSocketUtil.writePacket
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.Connection
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.isClosed
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

class KtorSocketClient @Inject constructor() : SocketClient {

    private val _connectionEvents =
        MutableSharedFlow<ConnectionEvent>(replay = 16, extraBufferCapacity = 64)
    override val connectionEvents = _connectionEvents.asSharedFlow()

    private val _messageEvent =
        MutableSharedFlow<MessageEvent>(replay = 16, extraBufferCapacity = 64)
    override val messageEvents = _messageEvent.asSharedFlow()

    private lateinit var serverConnection: Connection
    private var selectorManager: SelectorManager? = null

    override suspend fun startSession(host: String, port: Int) {
        try {
            setupClient(host, port)
            launchServerReadLoop()
        } catch (e: CancellationException) {
            Log.w(TAG, "Client connection attempt cancelled.")
            withContext(NonCancellable) {
                _connectionEvents.emit(ConnectionEvent.Disconnected(host))
            }
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "SocketClient: Exception in session: ${e.message}", e)
            _connectionEvents.emit(ConnectionEvent.Error(host, e.message.toString()))
        } finally {
            cleanup()
        }
    }

    override suspend fun disconnect() = coroutineScope {
        cleanup()
        // Emit disconnected if we had a connection
        if (::serverConnection.isInitialized) {
            _connectionEvents.emit(ConnectionEvent.Disconnected("Legacy"))
        }
    }

    override suspend fun sendToServer(data: String): Boolean {
        return try {
            if (!::serverConnection.isInitialized || serverConnection.socket.isClosed) {
                Log.e(TAG, "SocketClient: Cannot send, socket closed.")
                return false
            }
            serverConnection.output.writePacket(data)
            _messageEvent.emit(
                MessageEvent.Sent(serverConnection.socket.remoteAddress.toString(), data)
            )
            Log.d(TAG, "sendToServer: NOT DEADLOCKED!")
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "SocketClient: Failed to send data", e)
            false
        }
    }

    private suspend fun setupClient(host: String, port: Int) {
        Log.d(TAG, "Connecting to server at $host:$port...")
        selectorManager = SelectorManager(Dispatchers.IO)
        val socket = aSocket(selectorManager!!).tcp().connect(host, port) {
            keepAlive = true
            noDelay = true
        }
        serverConnection =
            Connection(socket, socket.openReadChannel(), socket.openWriteChannel(autoFlush = true))
        _connectionEvents.emit(ConnectionEvent.Connected(host))
        Log.d(TAG, "Connected to server: ${socket.remoteAddress}")
    }

    private suspend fun launchServerReadLoop() = coroutineScope {
        launch {
            try {
                while (isActive && !serverConnection.socket.isClosed) {
                    val line = serverConnection.input.readPacket()
                    if (line == null) {
                        Log.w(TAG, "SocketClient: Server closed connection (EOF).")
                        throw CancellationException("Server disconnected.")
                    }
                    Log.d(TAG, "SocketClient: Read packet: $line")
                    _messageEvent.emit(
                        MessageEvent.Received(
                            serverConnection.socket.remoteAddress.toString(),
                            line
                        )
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "SocketClient: Read Loop Error: ${e.message}")
                throw IOException("Read Error", e)
            }
        }
    }

    private suspend fun cleanup() {
        withContext(NonCancellable) {
            try {
                if (::serverConnection.isInitialized && !serverConnection.socket.isClosed) {
                    serverConnection.socket.close()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error closing socket", e)
            }

            try {
                selectorManager?.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error closing selector", e)
            }
        }
    }
}