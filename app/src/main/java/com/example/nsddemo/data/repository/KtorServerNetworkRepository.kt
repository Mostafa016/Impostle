package com.example.nsddemo.data.repository

import android.util.Log
import com.example.nsddemo.core.util.Debugging.TAG
import com.example.nsddemo.data.local.network.nsd.registration.NetworkRegistration
import com.example.nsddemo.data.local.network.nsd.registration.NsdRegistrationState
import com.example.nsddemo.data.local.network.socket.ConnectionEvent
import com.example.nsddemo.data.local.network.socket.MessageEvent
import com.example.nsddemo.data.local.network.socket.server.ServerListeningState
import com.example.nsddemo.data.local.network.socket.server.SocketServer
import com.example.nsddemo.data.util.PlayerConnectionEvent
import com.example.nsddemo.data.util.ServerState
import com.example.nsddemo.domain.model.Player
import com.example.nsddemo.domain.repository.ServerNetworkRepository
import com.example.nsddemo.domain.util.ClientMessage
import com.example.nsddemo.domain.util.NetworkJson
import com.example.nsddemo.domain.util.ServerMessage
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.encodeToString
import javax.inject.Inject

class KtorServerNetworkRepository @Inject constructor(
    private val networkRegistration: NetworkRegistration,
    private val socketServer: SocketServer,
) : ServerNetworkRepository {
    private val _serverState = MutableStateFlow<ServerState>(ServerState.Idle)
    override val serverState: Flow<ServerState> = _serverState

    private val _playerToClientId: MutableStateFlow<Map<Player, String>> =
        MutableStateFlow(emptyMap())
    override val playerToClientId: StateFlow<Map<Player, String>> = _playerToClientId.asStateFlow()

    override val incomingMessages: Flow<Pair<String, ClientMessage>> =
        socketServer.messageEvents.filterIsInstance(MessageEvent.Received::class)
            .mapNotNull { event ->
                try {
                    event.clientId to NetworkJson.decodeFromString<ClientMessage>(event.data)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse message from ${event.clientId}: ${e.message}")
                    null // Skip bad messages
                }
            }
    override val outGoingMessages: Flow<Pair<String, ServerMessage>> =
        socketServer.messageEvents.filterIsInstance(MessageEvent.Sent::class)
            .map { it.clientId to NetworkJson.decodeFromString<ServerMessage>(it.data) }

    private val connectionEvents: Flow<PlayerConnectionEvent.PlayerConnected> =
        incomingMessages
            .filter { (_, msg) -> msg is ClientMessage.RegisterPlayer }
            .map { (clientId, msg) ->
                val registerMsg = msg as ClientMessage.RegisterPlayer
                PlayerConnectionEvent.PlayerConnected(
                    id = clientId,
                    playerName = registerMsg.playerName
                )
            }

    private val disconnectionEvents: Flow<PlayerConnectionEvent.PlayerDisconnected> =
        socketServer.connectionEvents
            .filterIsInstance<ConnectionEvent.Disconnected>()
            .mapNotNull { event ->
                // Find the player associated with this ID
                val entry = _playerToClientId.value.entries.find { it.value == event.id }
                // Only emit if we actually knew about this player
                if (entry != null) {
                    PlayerConnectionEvent.PlayerDisconnected(entry.value, entry.key)
                } else {
                    null
                }
            }
            .onEach { event ->
                disassociatePlayerFromClient(event.player)
            }
    override val playerConnectionEvents: Flow<PlayerConnectionEvent> =
        merge(connectionEvents, disconnectionEvents)

    override suspend fun start(gameCode: String): Unit = coroutineScope {
        // Start Socket
        launch { socketServer.startListening() }

        // Wait for Binding
        val listeningState = try {
            withTimeout(5000L) {
                socketServer.listeningState.first { it !is ServerListeningState.Idle }
            }
        } catch (e: TimeoutCancellationException) {
            _serverState.value = ServerState.Error("Failed to bind socket: Timeout")
            return@coroutineScope
        }
        when (listeningState) {
            is ServerListeningState.Listening -> {
                handleRegistration(gameCode, listeningState.port)
            }

            is ServerListeningState.Error -> {
                _serverState.value = ServerState.Error(listeningState.message)
            }

            else -> {
                Log.wtf(TAG, "Unexpected state: $listeningState while starting server")
            }
        }
    }

    override fun associatePlayerWithClient(clientId: String, player: Player) {
        _playerToClientId.update { currentMap ->
            currentMap + (player to clientId)
        }
    }

    override fun disassociatePlayerFromClient(player: Player) {
        _playerToClientId.update { currentMap ->
            currentMap - player
        }
    }

    override suspend fun sendToPlayer(player: Player, message: ServerMessage) {
        socketServer.sendToClient(
            clientId = playerToClientId.value[player]!!,
            data = NetworkJson.encodeToString<ServerMessage>(message)
        )
    }

    override suspend fun sendToAllPlayers(message: ServerMessage) {
        socketServer.sendToAll(data = NetworkJson.encodeToString<ServerMessage>(message))
    }

    override fun cancelAdvertising() {
        networkRegistration.unregisterService()
    }

    override suspend fun stop() {
        socketServer.stopListening()
        networkRegistration.unregisterService()
        _serverState.value = ServerState.Idle
        _playerToClientId.value = emptyMap()
    }


    //region start() Helpers
    private suspend fun handleRegistration(gameCode: String, port: Int) {
        networkRegistration.registerService(gameCode, port)

        try {
            val state = withTimeout(5000L) {
                networkRegistration.registrationState.first {
                    it is NsdRegistrationState.Registered || it is NsdRegistrationState.Failed
                }
            }

            when (state) {
                is NsdRegistrationState.Registered -> {
                    _serverState.value = ServerState.Running(port, gameCode)
                }

                is NsdRegistrationState.Failed -> {
                    _serverState.value = ServerState.Error("NSD Failed: ${state.error}")
                    socketServer.stopListening()
                }

                else -> {}
            }
        } catch (e: TimeoutCancellationException) {
            _serverState.value = ServerState.Error("NSD Registration Timed Out")
            socketServer.stopListening()
        }
    }
    //endregion
}