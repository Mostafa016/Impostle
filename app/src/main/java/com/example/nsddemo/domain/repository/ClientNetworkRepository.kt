package com.example.nsddemo.domain.repository

import com.example.nsddemo.data.util.ClientState
import com.example.nsddemo.domain.util.ClientMessage
import com.example.nsddemo.domain.util.ServerMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface ClientNetworkRepository {
    val clientState: StateFlow<ClientState>
    val incomingMessages: Flow<Pair<String, ClientMessage>>
    val outGoingMessages: Flow<Pair<String, ServerMessage>>

    suspend fun connect(gameCode: String)
    suspend fun disconnect()
    suspend fun sendToServer(message: ClientMessage): Boolean
}