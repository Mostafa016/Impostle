package com.mostafa.impostle.data.local.network.socket.server

import com.mostafa.impostle.data.local.network.socket.ConnectionEvent
import com.mostafa.impostle.data.local.network.socket.MessageEvent
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface SocketServer {
    val listeningState: StateFlow<ServerListeningState>
    val connectionEvents: SharedFlow<ConnectionEvent>
    val messageEvents: SharedFlow<MessageEvent>

    suspend fun startListening()

    fun stopListening()

    suspend fun sendToClient(
        clientId: String,
        data: String,
    ): Boolean

    suspend fun sendToAll(data: String): Boolean

    fun disconnectClient(clientId: String)
}
