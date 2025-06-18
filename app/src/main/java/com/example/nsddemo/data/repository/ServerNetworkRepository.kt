package com.example.nsddemo.data.repository

import com.example.nsddemo.data.util.PlayerConnectionEvent
import com.example.nsddemo.data.util.ServerState
import com.example.nsddemo.domain.model.Player
import com.example.nsddemo.domain.util.ClientMessage
import com.example.nsddemo.domain.util.ServerMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface ServerNetworkRepository {
    val serverState: Flow<ServerState>
    val playerConnectionEvents: Flow<PlayerConnectionEvent>
    val incomingMessages: Flow<Pair<String, ClientMessage>> // clientId, message
    val outGoingMessages: Flow<Pair<String, ServerMessage>> // clientId, message
    val playerToClientId: StateFlow<Map<Player, String>> // player, clientId

    suspend fun start(gameCode: String)
    fun associatePlayerWithClient(clientId: String, player: Player)
    fun disassociatePlayerFromClient(player: Player)
    suspend fun sendToPlayer(player: Player, message: ServerMessage)
    suspend fun sendToAllPlayers(message: ServerMessage)
    fun cancelAdvertising()
    suspend fun stop()
}