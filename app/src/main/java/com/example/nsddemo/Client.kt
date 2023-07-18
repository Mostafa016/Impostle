package com.example.nsddemo


import android.util.Log
import com.example.nsddemo.Debugging.TAG
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.Connection
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.network.sockets.toJavaAddress
import io.ktor.util.network.address
import io.ktor.utils.io.writeStringUtf8
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.*
import kotlin.system.*

object Client {
    var serverIpAddress: String = ""

    suspend fun run(
        hostName: String,
        port: Int,
        handleMessages: suspend (connection: Connection) -> Unit
    ) = coroutineScope {
        val selectorManager = SelectorManager(Dispatchers.IO)
        Log.d(TAG, "Connecting to server...")
        val socket = aSocket(selectorManager).tcp().connect(hostName, port)
        Log.d(TAG, "Client address: ${socket.localAddress}")
        Log.d(TAG, "Connected to server ${socket.remoteAddress}")

        val receiveChannel = socket.openReadChannel()
        val sendChannel = socket.openWriteChannel(autoFlush = true)
        val connection = Connection(socket, receiveChannel, sendChannel)
        Log.d(TAG, "created receive and send channels")
        launch {
            try {
                    handleMessages(connection)
            } catch (e: Exception) {
                Log.e(TAG, e.message.toString())
                Log.e(TAG, e.stackTraceToString())
                Log.e(TAG, "Server closed a connection")
                delay(1000)
                socket.close()
                selectorManager.close()
                exitProcess(0)
            }
        }
    }
}