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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.nio.channels.ClosedChannelException
import java.nio.channels.ClosedSelectorException
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

class KtorSocketClient
    @Inject
    constructor() : SocketClient {
        private val _connectionEvents =
            MutableSharedFlow<ConnectionEvent>(replay = 16, extraBufferCapacity = 64)
        override val connectionEvents = _connectionEvents.asSharedFlow()

        private val _messageEvents =
            MutableSharedFlow<MessageEvent>(replay = 16, extraBufferCapacity = 64)
        override val messageEvents = _messageEvents.asSharedFlow()

        private lateinit var serverConnection: Connection
        private var selectorManager: SelectorManager? = null

        override suspend fun startSession(
            host: String,
            port: Int,
        ) {
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

        override suspend fun disconnect(): Unit =
            coroutineScope {
                try {
                    if (::serverConnection.isInitialized) {
                        _connectionEvents.emit(ConnectionEvent.Disconnected(serverConnection.socket.remoteAddress.toString()))
                    }
                } catch (e: ClosedChannelException) {
                    Log.w(TAG, "KtorSocketClient: Channel already closed")
                } finally {
                    cleanup()
                }
            }

        override suspend fun sendToServer(data: String): Boolean {
            return try {
                if (!::serverConnection.isInitialized || serverConnection.socket.isClosed) {
                    Log.e(TAG, "SocketClient: Cannot send, socket closed.")
                    return false
                }
                serverConnection.output.writePacket(data)
                _messageEvents.emit(
                    MessageEvent.Sent(serverConnection.socket.remoteAddress.toString(), data),
                )
                true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "SocketClient: Failed to send data", e)
                false
            }
        }

        private suspend fun setupClient(
            host: String,
            port: Int,
        ) {
            Log.d(TAG, "Connecting to server at $host:$port...")
            selectorManager = SelectorManager(Dispatchers.IO)
            try {
                val socket =
                    aSocket(selectorManager!!).tcp().connect(host, port) {
                        keepAlive = true
                        noDelay = true
                    }
                serverConnection =
                    Connection(
                        socket,
                        socket.openReadChannel(),
                        socket.openWriteChannel(autoFlush = true),
                    )
                _connectionEvents.emit(ConnectionEvent.Connected(host))
                Log.d(TAG, "Connected to server: ${socket.remoteAddress}")
            } catch (e: ClosedSelectorException) {
                // This happens if cleanup() is called while connect() is suspended
                throw IOException("Connection attempt cancelled (Selector closed)", e)
            }
        }

        private suspend fun launchServerReadLoop() =
            coroutineScope {
                launch {
                    try {
                        while (isActive && !serverConnection.socket.isClosed) {
                            val line = serverConnection.input.readPacket()
                            if (line == null) {
                                Log.i(TAG, "SocketClient: Server closed connection (EOF).")
                                _connectionEvents.emit(
                                    ConnectionEvent.Disconnected(
                                        serverConnection.socket.remoteAddress.toString(),
                                    ),
                                )
                                break
                            }
                            Log.d(TAG, "SocketClient: Read packet: $line")
                            _messageEvents.emit(
                                MessageEvent.Received(
                                    serverConnection.socket.remoteAddress.toString(),
                                    line,
                                ),
                            )
                        }
                    } catch (e: CancellationException) {
                        Log.d(TAG, "SocketClient: Read loop cancelled")
                        throw e
                    } catch (e: Exception) {
                        // Handle actual IO errors (timeout, malformed, etc)
                        Log.e(TAG, "SocketClient: Read Loop Error: ${e.message}", e)
                        _connectionEvents.emit(
                            ConnectionEvent.Error(
                                serverConnection.socket.remoteAddress.toString(),
                                e.message ?: "Unknown Error",
                            ),
                        )
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
