package com.example.nsddemo.domain.repository

import com.example.nsddemo.domain.model.ClientMessage
import com.example.nsddemo.domain.model.PlayerConnectionEvent
import com.example.nsddemo.domain.model.ServerMessage
import com.example.nsddemo.domain.model.ServerState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface ServerNetworkRepository {
    val serverState: StateFlow<ServerState>
    val playerConnectionEvents: Flow<PlayerConnectionEvent>
    val incomingMessages: Flow<Pair<String, ClientMessage>> // clientId, message
    val outGoingMessages: Flow<Pair<String, ServerMessage>> // clientId, message

    suspend fun start(gameCode: String)
    suspend fun sendToPlayer(playerId: String, message: ServerMessage)
    suspend fun sendToAllPlayers(message: ServerMessage)
    fun disconnectPlayer(playerId: String)
    fun cancelAdvertising()
    suspend fun stop()
}