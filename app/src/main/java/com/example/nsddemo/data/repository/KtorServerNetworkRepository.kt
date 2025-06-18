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
import com.example.nsddemo.domain.util.ClientMessage
import com.example.nsddemo.domain.util.NetworkJson
import com.example.nsddemo.domain.util.ServerMessage
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import javax.inject.Inject

class KtorServerNetworkRepository @Inject constructor(
    private val networkRegistration: NetworkRegistration,
    private val socketServer: SocketServer,
) : ServerNetworkRepository {
    private val _serverState = MutableStateFlow<ServerState>(ServerState.Idle)
    override val serverState: Flow<ServerState> = _serverState

    override val incomingMessages: Flow<Pair<String, ClientMessage>> =
        socketServer.messageEvents.filterIsInstance(MessageEvent.Received::class)
            .map { it.clientId to NetworkJson.decodeFromString<ClientMessage>(it.data) }
    override val outGoingMessages: Flow<Pair<String, ServerMessage>> =
        socketServer.messageEvents.filterIsInstance(MessageEvent.Sent::class)
            .map { it.clientId to NetworkJson.decodeFromString<ServerMessage>(it.data) }

    private val connectionEvents: Flow<PlayerConnectionEvent.PlayerConnected> =
        socketServer.connectionEvents.filterIsInstance(ConnectionEvent.Connected::class)
            .zip(incomingMessages.filterIsInstance(ClientMessage.RegisterPlayer::class)) { connectionEvent, playerNameMessage ->
                connectionEvent.id to playerNameMessage.playerName
            }.map { (clientId, playerName) ->
                PlayerConnectionEvent.PlayerConnected(
                    clientId,
                    playerName
                )
            }
    private val disconnectionEvents: Flow<PlayerConnectionEvent.PlayerDisconnected> =
        socketServer.connectionEvents.filterIsInstance(ConnectionEvent.Disconnected::class)
            .map { serverDisconnectionEvent ->
                val playerToClientIdMap = _playerToClientId.value
                val (player, clientId) = playerToClientIdMap.entries.find { it.value == serverDisconnectionEvent.id }!!
                PlayerConnectionEvent.PlayerDisconnected(clientId, player)
            }.onEach { serverDisconnectionState ->
                disassociatePlayerFromClient(serverDisconnectionState.player)
            }
    override val playerConnectionEvents: Flow<PlayerConnectionEvent> =
        merge(connectionEvents, disconnectionEvents)

    private val _playerToClientId: MutableStateFlow<Map<Player, String>> =
        MutableStateFlow(emptyMap())
    override val playerToClientId: StateFlow<Map<Player, String>> = _playerToClientId.asStateFlow()

    override suspend fun start(gameCode: String): Unit = coroutineScope {
        launch { socketServer.startListening() }
        val serverListeningState =
            socketServer.listeningState.first { it !is ServerListeningState.Idle }
        when (serverListeningState) {
            is ServerListeningState.Listening -> {
                val port = serverListeningState.port
                launch {
                    handleRegistration(gameCode, port)
                }
            }

            is ServerListeningState.Error -> {
                val socketServerErrorState =
                    socketServer.listeningState.value as ServerListeningState.Error
                _serverState.value = ServerState.Error(socketServerErrorState.message)
            }

            else -> {
                Log.wtf(TAG, "Unexpected state: $serverListeningState while starting server")
            }
        }
    }

    override fun associatePlayerWithClient(clientId: String, player: Player) {
        _playerToClientId.update {
            it.toMutableMap().apply { put(player, clientId) }.toMap()
        }
    }

    override fun disassociatePlayerFromClient(player: Player) {
        // Could be used for a change color feature
        _playerToClientId.update {
            it.toMutableMap().apply { remove(player) }.toMap()
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
        val serverListeningState =
            socketServer.listeningState.first { it is ServerListeningState.Idle || it is ServerListeningState.Error }
        when (serverListeningState) {
            is ServerListeningState.Idle -> {
                _serverState.value = ServerState.Idle
            }

            is ServerListeningState.Error -> {
                _serverState.value = ServerState.Error(serverListeningState.message)
            }

            else -> {
                /* Do nothing */
                Log.wtf(TAG, "Unexpected state: $serverListeningState while stopping the server")
            }
        }
        if (networkRegistration.registrationState.value !is NsdRegistrationState.Registered) {
            Log.i(TAG, "Service is not registered, skipping cancellation.")
            return
        }
        networkRegistration.unregisterService()
        val registrationState =
            networkRegistration.registrationState.first { it is NsdRegistrationState.UnRegistered || it is NsdRegistrationState.Failed }
        when (registrationState) {
            is NsdRegistrationState.UnRegistered -> {
                Log.i(TAG, "Stopped registration successfully")
            }

            is NsdRegistrationState.Failed -> {
                _serverState.value = ServerState.Error(registrationState.error)
            }

            else -> {
                Log.wtf(TAG, "Unexpected state $registrationState while stopping the server")
            }
        }

    }


    //region start() Helpers
    private suspend fun handleRegistration(gameCode: String, port: Int) {
        networkRegistration.registerService(gameCode, port)

        val state =
            networkRegistration.registrationState.first { state -> state is NsdRegistrationState.Registered || state is NsdRegistrationState.Failed }
        when (state) {
            is NsdRegistrationState.Registered -> {
                _serverState.value = ServerState.Running(port = port, gameCode = gameCode)
            }

            is NsdRegistrationState.Failed -> {
                _serverState.value = ServerState.Error(state.error)
                socketServer.stopListening()
            }

            else -> {
                /* Do nothing */
                Log.wtf(TAG, "Unexpected state: $state while registering service")
            }
        }
    }
    //endregion
}