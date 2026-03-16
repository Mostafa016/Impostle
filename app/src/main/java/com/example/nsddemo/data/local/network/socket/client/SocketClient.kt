package com.example.nsddemo.data.local.network.socket.client

import com.example.nsddemo.data.local.network.socket.ConnectionEvent
import com.example.nsddemo.data.local.network.socket.MessageEvent
import kotlinx.coroutines.flow.SharedFlow

interface SocketClient {
    val connectionEvents: SharedFlow<ConnectionEvent>
    val messageEvents: SharedFlow<MessageEvent>

    suspend fun startSession(
        host: String,
        port: Int,
    )

    suspend fun disconnect()

    suspend fun sendToServer(data: String): Boolean
}
