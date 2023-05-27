package com.example.nsddemo


import android.util.Log
import com.example.nsddemo.Debugging.TAG
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.writeStringUtf8
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.*
import kotlin.system.*

object Client {
    var serverIpAddress: String = ""

    suspend fun run(hostName: String, port: Int) = coroutineScope {
        val selectorManager = SelectorManager(Dispatchers.IO)
        Log.d(TAG, "Connecting to server...")
        val socket = aSocket(selectorManager).tcp().connect(hostName, port)
        Log.d(TAG, "Client address: ${socket.localAddress}")
        Log.d(TAG, "Connected to server ${socket.remoteAddress}")

        val receiveChannel = socket.openReadChannel()
        val sendChannel = socket.openWriteChannel(autoFlush = true)
        Log.d(TAG, "created receive and send channels")
        launch {
            while (true) {
                Log.d(TAG, "Reading received message from server...")
                var greeting: String? = null
                try {
                    Log.d(TAG, "Before receiveChannel.readUTF8Line")
                    //TODO: Check if any other method is used from the wrong package
                    Log.d(TAG, "Socket ${socket.remoteAddress}")
                    greeting = receiveChannel.readUTF8Line()
                    Log.d(TAG, "Server sent: $greeting")
                } catch (e: Exception) {
                    Log.e(TAG, e.message.toString())
                }
                Log.d(TAG, "After try catch")
                if (greeting != null) {
                    Log.d(TAG, greeting)
                } else {
                    Log.e(TAG, "Server closed a connection")
                    delay(1000)
                    socket.close()
                    selectorManager.close()
                    exitProcess(0)
                }
            }
        }

        Log.d(TAG, "Sending message to server...")
        //TODO: Every message should end in "\n" to work
        sendChannel.writeStringUtf8("Jeff\n")
        Log.d(TAG, "Message sent to server")
//        while (true) {
//            Log.d(TAG, "Enter message to send")
//            val myMessage = readln()
//            Log.d(TAG, "Writing message to send channel")
//            sendChannel.writeStringUtf8("$myMessage\n")
//        }

    }
}