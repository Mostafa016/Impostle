package com.example.nsddemo

import android.util.Log
import com.example.nsddemo.Debugging.TAG
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.util.network.address
import io.ktor.util.network.port
import kotlinx.coroutines.*
import io.ktor.utils.io.writeStringUtf8
import io.ktor.utils.io.readUTF8Line


object Server {
    val players: MutableMap<String, Player> = mutableMapOf()

    private var serverSocket: ServerSocket? = null
    private var LOCK = Any()
    fun initServerSocket(serverIP: String): Int {
        val selectorManager = SelectorManager(Dispatchers.IO)
        serverSocket = aSocket(selectorManager).tcp().bind(serverIP, 0)
        val port = serverSocket!!.localAddress.toJavaAddress().port
        Log.d(TAG, "Server is listening at ${serverSocket?.localAddress}")
        return port
    }

    suspend fun run() = coroutineScope {
        while (true) {
            val socket = serverSocket!!.accept()
            Log.d(TAG, "Accepted ${socket.remoteAddress}")
            launch {
                val receiveChannel = socket.openReadChannel()
                val sendChannel = socket.openWriteChannel(autoFlush = true)
                Log.d(TAG, "Sending message to client...")
                sendChannel.writeStringUtf8("Please enter your name\n")
                Log.d(TAG, "Message sent to client")
                try {
                    while (true) {
                        Log.d(TAG, "Waiting for client response...")
                        val name = receiveChannel.readUTF8Line()
                        Log.d(TAG, "Received from client: $name")
                        synchronized(LOCK) {
                            players.put(
                                key = socket.remoteAddress.toJavaAddress().address,
                                value = Player(
                                    socket.remoteAddress.toJavaAddress().address,
                                    name!!,
                                    "FF0000"
                                )
                            )
                        }
                        sendChannel.writeStringUtf8("Hello, $name!\n")
                    }
                } catch (e: Throwable) {
                    Log.e(TAG, e.message.toString())
                    Log.e(TAG, "Closing connection")
                    socket.close()
                }
            }
        }
    }
}
