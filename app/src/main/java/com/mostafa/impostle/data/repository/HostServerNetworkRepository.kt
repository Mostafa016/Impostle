package com.mostafa.impostle.data.repository

import android.util.Log
import com.mostafa.impostle.core.util.Debugging.TAG
import com.mostafa.impostle.data.local.network.LoopbackDataSource
import com.mostafa.impostle.data.local.network.nsd.registration.NetworkRegistration
import com.mostafa.impostle.data.local.network.nsd.registration.NsdRegistrationState
import com.mostafa.impostle.data.local.network.socket.ConnectionEvent
import com.mostafa.impostle.data.local.network.socket.MessageEvent
import com.mostafa.impostle.data.local.network.socket.server.ServerListeningState
import com.mostafa.impostle.data.local.network.socket.server.SocketServer
import com.mostafa.impostle.data.util.PingTimings.PING_INTERVAL_MS
import com.mostafa.impostle.data.util.PingTimings.PING_TIMEOUT_MS
import com.mostafa.impostle.domain.model.ClientMessage
import com.mostafa.impostle.domain.model.NetworkJson
import com.mostafa.impostle.domain.model.PlayerConnectionEvent
import com.mostafa.impostle.domain.model.ServerMessage
import com.mostafa.impostle.domain.model.ServerState
import com.mostafa.impostle.domain.repository.ServerNetworkRepository
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.SerializationException
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

class HostServerNetworkRepository
    @Inject
    constructor(
        private val networkRegistration: NetworkRegistration,
        private val socketServer: SocketServer,
        private val loopbackDataSource: LoopbackDataSource,
    ) : ServerNetworkRepository {
        private val _serverState = MutableStateFlow<ServerState>(ServerState.Idle)
        override val serverState: StateFlow<ServerState> = _serverState

        private val clientLastSeen = ConcurrentHashMap<String, Long>()
        private val sessionManager = SessionManager()

        override val incomingMessages: Flow<Pair<String, ClientMessage>> =
            merge(
                socketServer.messageEvents
                    .filterIsInstance(MessageEvent.Received::class)
                    .mapNotNull { event ->
                        try {
                            clientLastSeen[event.clientId] = System.currentTimeMillis()
                            val message = NetworkJson.decodeFromString<ClientMessage>(event.data)
                            if (message is ClientMessage.Heartbeat) return@mapNotNull null
                            handleIncomingSession(message, TransportEndpoint.Network(event.clientId))
                        } catch (e: SerializationException) {
                            Log.e(
                                TAG,
                                "Failed to parse message from ${event.clientId}: ${e.message}",
                                e,
                            )
                            null
                        } catch (e: IllegalArgumentException) {
                            Log.e(
                                TAG,
                                "Failed to parse message from ${event.clientId}: ${e.message}",
                                e,
                            )
                            null
                        }
                    },
                loopbackDataSource.clientToServer.mapNotNull { (_, message) ->
                    handleIncomingSession(message, TransportEndpoint.Loopback)
                },
            )

        private fun handleIncomingSession(
            message: ClientMessage,
            transport: TransportEndpoint,
        ): Pair<String, ClientMessage>? {
            if (message is ClientMessage.RegisterPlayer) {
                sessionManager.registerSession(domainId = message.playerId, transport = transport)
                return message.playerId to message
            }

            val domainId = sessionManager.getDomainId(transport)
            if (domainId == null) {
                Log.w(TAG, "Unregistered message from $transport. Dropping.")
                return null
            }

            return domainId to message
        }

        override val outGoingMessages: Flow<Pair<String, ServerMessage>> =
            socketServer.messageEvents
                .filterIsInstance(MessageEvent.Sent::class)
                .map {
                    LoopbackDataSource.LOCAL_HOST_CLIENT_ID to
                        NetworkJson.decodeFromString<ServerMessage>(
                            it.data,
                        )
                }

        private val connectionEvents: Flow<PlayerConnectionEvent.PlayerConnected> =
            incomingMessages
                .filter { (_, message) -> message is ClientMessage.RegisterPlayer }
                .map { (_, message) ->
                    val registerMsg = message as ClientMessage.RegisterPlayer
                    PlayerConnectionEvent.PlayerConnected(
                        id = registerMsg.playerId,
                        playerName = registerMsg.playerName,
                    )
                }

        private val disconnectionEvents: Flow<PlayerConnectionEvent.PlayerDisconnected> =
            socketServer.connectionEvents
                .filterIsInstance<ConnectionEvent.Disconnected>()
                .mapNotNull { event ->
                    clientLastSeen.remove(event.clientId)
                    val playerTransport = TransportEndpoint.Network(event.clientId)
                    val playerId = sessionManager.getDomainId(playerTransport)
                    if (playerId != null) {
                        sessionManager.clearSession(playerTransport)
                        PlayerConnectionEvent.PlayerDisconnected(playerId)
                    } else {
                        Log.e(TAG, "Unknown player with clientId ${event.clientId} disconnected")
                        null
                    }
                }

        override val playerConnectionEvents: Flow<PlayerConnectionEvent> =
            merge(connectionEvents, disconnectionEvents)

        override suspend fun start(gameCode: String): Unit =
            coroutineScope {
                launch { socketServer.startListening() }

                val listeningState =
                    try {
                        withTimeout(CONNECTION_STEP_TIMEOUT) {
                            socketServer.listeningState.first { it !is ServerListeningState.Idle }
                        }
                    } catch (e: TimeoutCancellationException) {
                        _serverState.value = ServerState.Error("Failed to bind socket: Timeout")
                        return@coroutineScope
                    }
                when (listeningState) {
                    is ServerListeningState.Listening -> {
                        handleRegistration(gameCode, listeningState.port)
                        // NEW: Launch the server heartbeat loop
                        launch { startHeartbeatLoop() }
                        launch {
                            socketServer.listeningState.collect { state ->
                                // If we encounter an error AFTER startup (e.g. OS kills socket, interface down)
                                if (state is ServerListeningState.Error) {
                                    _serverState.value =
                                        ServerState.Error("Server Socket Failed: ${state.message}")
                                }
                                // Note: We don't handle Idle here because stop() sets Idle manually.
                            }
                        }
                    }

                    is ServerListeningState.Error -> {
                        _serverState.value = ServerState.Error(listeningState.message)
                    }

                    else -> {
                        Log.wtf(TAG, "Unexpected state: $listeningState while starting server")
                    }
                }
            }

        private suspend fun startHeartbeatLoop() {
            val encodedPing = NetworkJson.encodeToString<ServerMessage>(ServerMessage.Heartbeat)

            while (true) {
                delay(PING_INTERVAL_MS)
                val now = System.currentTimeMillis()

                // Get all network clients (exclude the Host's loopback connection)
                val networkClients = sessionManager.getAllNetworkClientIds()

                for (clientId in networkClients) {
                    // 1. Send Ping
                    socketServer.sendToClient(clientId, encodedPing)

                    // 2. Check for Timeout
                    val lastSeen = clientLastSeen[clientId] ?: now
                    if (now - lastSeen > PING_TIMEOUT_MS) {
                        Log.w(TAG, "Client $clientId heartbeat timeout. Forcing disconnect.")
                        socketServer.disconnectClient(clientId)
                    }
                }
            }
        }

        override suspend fun sendToPlayer(
            playerId: String,
            message: ServerMessage,
        ) {
            val transport = sessionManager.getTransport(playerId) ?: return
            Log.d(TAG, "sendToPlayer: Sending to $playerId")

            when (transport) {
                is TransportEndpoint.Loopback -> {
                    Log.d(TAG, "sendToPlayer: [Loopback] Sending to $playerId")
                    loopbackDataSource.serverToClient.emit(
                        LoopbackDataSource.LOCAL_HOST_CLIENT_ID to message,
                    )
                }

                is TransportEndpoint.Network -> {
                    Log.d(TAG, "sendToPlayer: [Remote] Sending to $playerId")
                    socketServer.sendToClient(
                        transport.clientId,
                        NetworkJson.encodeToString(message),
                    )
                }
            }
        }

        override suspend fun sendToAllPlayers(message: ServerMessage) {
            socketServer.sendToAll(data = NetworkJson.encodeToString<ServerMessage>(message))
            loopbackDataSource.serverToClient.emit(LoopbackDataSource.LOCAL_HOST_CLIENT_ID to message)
        }

        override fun disconnectPlayer(playerId: String) {
            val clientTransport =
                sessionManager.getTransport(playerId) as? TransportEndpoint.Network
            clientTransport?.let {
                socketServer.disconnectClient(it.clientId)
            }
        }

        override fun cancelAdvertising() {
            networkRegistration.unregisterService()
        }

        override suspend fun stop() {
            socketServer.stopListening()
            networkRegistration.unregisterService()
            sessionManager.clearALlSessions()
            _serverState.value = ServerState.Idle
        }

        //region start() Helpers
        private suspend fun handleRegistration(
            gameCode: String,
            port: Int,
        ) {
            networkRegistration.registerService(gameCode, port)

            try {
                val state =
                    withTimeout(CONNECTION_STEP_TIMEOUT) {
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

        //region --- Helper Classes/Interfaces ---
        private sealed interface TransportEndpoint {
            data object Loopback : TransportEndpoint

            data class Network(
                val clientId: String,
            ) : TransportEndpoint
        }

        private class SessionManager {
            // Maps UUID -> IP/Loopback
            private val idToTransport = ConcurrentHashMap<String, TransportEndpoint>()

            // Maps IP/Loopback -> UUID
            private val transportToId = ConcurrentHashMap<TransportEndpoint, String>()

            fun registerSession(
                domainId: String,
                transport: TransportEndpoint,
            ) {
                // 1. Check if this ID was previously at a different transport (Reconnection)
                val oldTransport = idToTransport[domainId]
                if (oldTransport != null && oldTransport != transport) {
                    transportToId.remove(oldTransport)
                }

                // 2. Atomic Update
                idToTransport[domainId] = transport
                transportToId[transport] = domainId
            }

            fun getDomainId(transport: TransportEndpoint): String? = transportToId[transport]

            fun getTransport(domainId: String): TransportEndpoint? = idToTransport[domainId]

            fun getAllNetworkClientIds(): List<String> =
                transportToId.keys
                    .filterIsInstance<TransportEndpoint.Network>()
                    .map { it.clientId }

            fun clearSession(transport: TransportEndpoint) {
                val id = transportToId.remove(transport)
                if (id != null) {
                    idToTransport.remove(id)
                }
            }

            fun clearALlSessions() {
                idToTransport.clear()
                transportToId.clear()
            }
        }
        //endregion

        // 1. Add Timing Constants
        companion object {
            const val CONNECTION_STEP_TIMEOUT = 2500L
        }
    }
