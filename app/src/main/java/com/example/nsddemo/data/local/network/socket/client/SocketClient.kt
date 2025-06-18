package com.example.nsddemo.data.local.network.socket.client

import com.example.nsddemo.data.local.network.socket.ConnectionEvent
import com.example.nsddemo.data.local.network.socket.MessageEvent
import kotlinx.coroutines.flow.Flow

interface SocketClient {
    val connectionEvents: Flow<ConnectionEvent>
    val messageEvents: Flow<MessageEvent>

    suspend fun connect(host: String, port: Int): Boolean
    suspend fun disconnect()
    suspend fun sendToServer(data: String): Boolean
}