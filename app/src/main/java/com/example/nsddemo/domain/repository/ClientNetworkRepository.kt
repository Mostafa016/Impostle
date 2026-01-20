package com.example.nsddemo.domain.repository

import com.example.nsddemo.domain.model.ClientMessage
import com.example.nsddemo.domain.model.ClientState
import com.example.nsddemo.domain.model.ServerMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface ClientNetworkRepository {
    val clientState: StateFlow<ClientState>
    val incomingMessages: Flow<Pair<String, ServerMessage>>
    val outGoingMessages: Flow<Pair<String, ClientMessage>>

    suspend fun connect(gameCode: String)
    suspend fun disconnect()
    suspend fun sendToServer(message: ClientMessage): Boolean
}