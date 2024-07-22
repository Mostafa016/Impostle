package com.example.nsddemo.data.local.network.socket

import android.util.Log
import com.example.nsddemo.core.util.Debugging.TAG
import com.example.nsddemo.domain.model.Player
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.Connection
import io.ktor.network.sockets.ServerSocket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.network.sockets.toJavaAddress
import io.ktor.util.network.port
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch


object Server {
    private var serverSocket: ServerSocket? = null
    val clients: MutableMap<Connection, Player> = mutableMapOf()
    var port: Int? = null
        private set

    fun initServerSocket(serverIP: String) {
        val selectorManager = SelectorManager(Dispatchers.IO)
        serverSocket = aSocket(selectorManager).tcp().bind(serverIP, 0)
        port = serverSocket!!.localAddress.toJavaAddress().port
        Log.d(TAG, "Server is listening at ${serverSocket?.localAddress}")
    }

    suspend fun run(handleMessages: suspend (connection: Connection) -> Unit) = coroutineScope {
        while (true) {
            val socket = serverSocket!!.accept()
            Log.d(TAG, "Accepted ${socket.remoteAddress}")
            launch {
                val receiveChannel = socket.openReadChannel()
                val sendChannel = socket.openWriteChannel(autoFlush = true)
                val connection = Connection(socket, receiveChannel, sendChannel)
                try {
                    while (true) {
                        handleMessages(connection)
                    }
                } catch (e: Throwable) {
                    Log.e(TAG, e.message.toString())
                    Log.e(TAG, e.stackTraceToString())
                    Log.e(TAG, "Closing connection")
                    socket.close()
                }
            }
        }
    }
}
