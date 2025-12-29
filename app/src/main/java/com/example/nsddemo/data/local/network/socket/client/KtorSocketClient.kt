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
import io.ktor.utils.io.charsets.MalformedInputException
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

class KtorSocketClient : SocketClient {

    private val _connectionEvents =
        MutableSharedFlow<ConnectionEvent>(extraBufferCapacity = EVENT_BUFFER_CAPACITY)
    override val connectionEvents = _connectionEvents.asSharedFlow()

    private val _messageEvent =
        MutableSharedFlow<MessageEvent>(extraBufferCapacity = EVENT_BUFFER_CAPACITY)
    override val messageEvents = _messageEvent.asSharedFlow()

    private lateinit var serverConnection: Connection

    override suspend fun startSession(host: String, port: Int) {
        try {
            setupClient(host, port)
            launchServerReadLoop()
        } catch (e: CancellationException) {
            Log.w(TAG, "Client connection attempt cancelled by parent job.")
            withContext(NonCancellable) {
                _connectionEvents.emit(ConnectionEvent.Disconnected(host))
            }
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "An unhandled exception occurred.", e)
            _connectionEvents.emit(ConnectionEvent.Error(host, e.message.toString()))
        } finally {
            if (::serverConnection.isInitialized && !serverConnection.socket.isClosed) {
                withContext(NonCancellable) {
                    serverConnection.socket.close()
                }
            }
        }
    }

    override suspend fun disconnect() = coroutineScope {
        if (serverConnection.socket.isClosed) return@coroutineScope
        launch { serverConnection.socket.close() }
        _connectionEvents.emit(ConnectionEvent.Disconnected(serverConnection.socket.remoteAddress.toString()))
    }

    override suspend fun sendToServer(data: String): Boolean {
        try {
            if (data.contains("\n")) {
                throw MalformedInputException("Data sent cannot contain new lines.")
            }
            serverConnection.output.writePacket(data)
            _messageEvent.emit(
                MessageEvent.Sent(
                    serverConnection.socket.remoteAddress.toString(), data
                )
            )
        } catch (e: MalformedInputException) {
            Log.e(TAG, "Data sent cannot contain new lines. Check serialization output.")
            return false
        } catch (e: NullPointerException) {
            Log.e(TAG, "Not connected to server.")
            return false
        }
        return true
    }

    //region connect() Helpers
    private suspend fun setupClient(host: String, port: Int) {
        Log.d(TAG, "Connecting to server...")
        val selectorManager = SelectorManager(Dispatchers.IO)
        val socket = aSocket(selectorManager).tcp().connect(host, port)
        serverConnection =
            Connection(socket, socket.openReadChannel(), socket.openWriteChannel(autoFlush = true))
        _connectionEvents.emit(ConnectionEvent.Connected(host))
        Log.d(TAG, "Client address: ${socket.localAddress}")
        Log.d(TAG, "Connected to server: ${socket.remoteAddress}")
    }

    private suspend fun launchServerReadLoop() = coroutineScope {
        launch {
            try {
                while (isActive && !serverConnection.socket.isClosed) {
                    val line = serverConnection.input.readPacket()
                        ?: throw CancellationException("Server disconnected, terminating connection.")
                    _messageEvent.emit(
                        MessageEvent.Received(
                            serverConnection.socket.remoteAddress.toString(), line
                        )
                    )
                }
            } catch (e: CancellationException) {
                Log.w(TAG, "Server disconnected.")
                withContext(NonCancellable) {
                    _connectionEvents.emit(ConnectionEvent.Disconnected(serverConnection.socket.remoteAddress.toString()))
                }
                throw e
            } catch (e: IOException) {
                Log.e(TAG, "An IO error occurred.")
                _connectionEvents.emit(
                    ConnectionEvent.Error(
                        serverConnection.socket.remoteAddress.toString(), e.message.toString()
                    )
                )
            } finally {
                Log.w(TAG, "Server read loop finished. Closing socket.")
                serverConnection.socket.close()
            }
        }
    }
    //endregion

    private companion object {
        const val EVENT_BUFFER_CAPACITY = 64
    }
}