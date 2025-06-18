package com.example.nsddemo.data.local.network.socket.server

import com.example.nsddemo.data.local.network.socket.ConnectionEvent
import com.example.nsddemo.data.local.network.socket.MessageEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface SocketServer {
    val listeningState: StateFlow<ServerListeningState>
    val connectionEvents: Flow<ConnectionEvent>
    val messageEvents: Flow<MessageEvent>

    suspend fun startListening()
    fun stopListening()
    suspend fun sendToClient(clientId: String, data: String): Result<Unit>
    suspend fun sendToAll(data: String): Result<Unit>
    fun disconnectClient(clientId: String)
}