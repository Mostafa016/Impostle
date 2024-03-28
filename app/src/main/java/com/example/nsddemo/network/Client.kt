package com.example.nsddemo.network


import android.util.Log
import androidx.compose.runtime.MutableState
import com.example.nsddemo.Debugging.TAG
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.Connection
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

object Client {
    var serverIpAddress: String = ""
    var replay = false
    suspend fun run(
        hostName: String,
        port: Int,
        hasFoundGame: MutableState<Boolean>,
        handleMessages: suspend (connection: Connection, hasFoundGame: MutableState<Boolean>) -> Unit
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
                do {
                    handleMessages(connection, hasFoundGame)
                } while (replay)
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