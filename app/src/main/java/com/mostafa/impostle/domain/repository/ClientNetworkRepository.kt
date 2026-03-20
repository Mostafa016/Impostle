package com.mostafa.impostle.domain.repository

import com.mostafa.impostle.domain.model.ClientMessage
import com.mostafa.impostle.domain.model.ClientState
import com.mostafa.impostle.domain.model.ServerMessage
import kotlinx.coroutines.flow.Flow

interface ClientNetworkRepository {
    val clientState: Flow<ClientState>
    val incomingMessages: Flow<Pair<String, ServerMessage>>
    val outGoingMessages: Flow<Pair<String, ClientMessage>>

    suspend fun connect(gameCode: String)

    suspend fun disconnect()

    suspend fun sendToServer(message: ClientMessage): Boolean
}
