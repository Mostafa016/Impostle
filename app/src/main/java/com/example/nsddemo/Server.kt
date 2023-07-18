package com.example.nsddemo

import android.util.Log
import com.example.nsddemo.Debugging.TAG
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.util.network.port
import kotlinx.coroutines.*
import io.ktor.utils.io.readUTF8Line


object Server {
    val players: MutableMap<Connection, Player> = mutableMapOf()

    private var serverSocket: ServerSocket? = null
    fun initServerSocket(serverIP: String): Int {
        val selectorManager = SelectorManager(Dispatchers.IO)
        serverSocket = aSocket(selectorManager).tcp().bind(serverIP, 0)
        val port = serverSocket!!.localAddress.toJavaAddress().port
        Log.d(TAG, "Server is listening at ${serverSocket?.localAddress}")
        return port
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
