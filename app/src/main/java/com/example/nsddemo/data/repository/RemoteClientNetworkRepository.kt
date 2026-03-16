package com.example.nsddemo.data.repository

import android.net.nsd.NsdServiceInfo
import android.util.Log
import com.example.nsddemo.core.util.Debugging.TAG
import com.example.nsddemo.data.local.network.nsd.discovery.NetworkDiscovery
import com.example.nsddemo.data.local.network.nsd.discovery.NsdDiscoveryEvent
import com.example.nsddemo.data.local.network.nsd.discovery.NsdDiscoveryState
import com.example.nsddemo.data.local.network.nsd.resolution.NetworkResolution
import com.example.nsddemo.data.local.network.nsd.resolution.NsdResolutionState
import com.example.nsddemo.data.local.network.socket.ConnectionEvent
import com.example.nsddemo.data.local.network.socket.MessageEvent
import com.example.nsddemo.data.local.network.socket.client.SocketClient
import com.example.nsddemo.di.IoDispatcher
import com.example.nsddemo.domain.model.ClientMessage
import com.example.nsddemo.domain.model.ClientState
import com.example.nsddemo.domain.model.NetworkJson
import com.example.nsddemo.domain.model.ServerMessage
import com.example.nsddemo.domain.repository.ClientNetworkRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.encodeToString
import javax.inject.Inject

class RemoteClientNetworkRepository
    @Inject
    constructor(
        private val networkDiscovery: NetworkDiscovery,
        private val networkResolution: NetworkResolution,
        private val socketClient: SocketClient,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : ClientNetworkRepository {
        private val _clientState = MutableStateFlow<ClientState>(ClientState.Idle)
        override val clientState = _clientState.asStateFlow()

        // Mapping Raw Socket events to Domain Sealed Classes
        override val incomingMessages: Flow<Pair<String, ServerMessage>> =
            socketClient.messageEvents
                .filterIsInstance(MessageEvent.Received::class)
                .onEach { Log.d(TAG, "RemoteClientNetworkRepository: Saw event $it") }
                .mapNotNull { event ->
                    try {
                        event.clientId to NetworkJson.decodeFromString<ServerMessage>(event.data)
                    } catch (e: Exception) {
                        if (e is CancellationException) {
                            Log.e(
                                TAG,
                                "RemoteClientNetworkRepository: incomingMessages collection cancelled",
                                e,
                            )
                            throw e
                        }
                        Log.e(TAG, "Failed to parse message: ${e.message}")
                        null // Drop bad packet, keep connection alive
                    }
                }

        override val outGoingMessages: Flow<Pair<String, ClientMessage>> =
            socketClient.messageEvents
                .filterIsInstance(MessageEvent.Sent::class)
                .map { it.clientId to NetworkJson.decodeFromString<ClientMessage>(it.data) }

        override suspend fun connect(gameCode: String) =
            coroutineScope {
                try {
                    // 1. Discovery Phase
                    val serviceInfo = performDiscovery(gameCode) ?: return@coroutineScope
                    Log.d(
                        TAG,
                        "RemoteClientRepository Connect: Performed discovery for service $serviceInfo",
                    )
                    // 2. Resolution Phase
                    val (host, port) = performResolution(serviceInfo, gameCode) ?: return@coroutineScope
                    Log.d(
                        TAG,
                        "RemoteClientRepository Connect: Performed resolution for service $serviceInfo with $host:$port",
                    )
                    // 3. Connection Phase
                    launch { performSocketConnection(host, port) }
                    Log.d(
                        TAG,
                        "RemoteClientRepository Connect: Performed server connection for service $serviceInfo with $host:$port",
                    )

                    // 4. Runtime Monitoring (Serialized)
                    // We listen to BOTH messages and connection events in a single sequential flow.
                    // This guarantees that if "LobbyClosed" arrives before "Disconnected",
                    // we process LobbyClosed first.
                    launch {
                        val mixedEvents =
                            merge(
                                socketClient.messageEvents,
                                socketClient.connectionEvents,
                            )

                        mixedEvents.collect { event ->
                            when (event) {
                                is MessageEvent.Received -> {
                                    // Pre-check for graceful exit signals to update state synchronously
                                    if (isGracefulExitSignal(event.data)) {
                                        Log.i(TAG, "Graceful Exit Signal detected. Setting Idle.")
                                        _clientState.value = ClientState.Idle
                                    }
                                }

                                is ConnectionEvent.Disconnected -> {
                                    // Because this collect block is sequential, if the message above
                                    // ran first, the state is ALREADY Idle.
                                    if (_clientState.value !is ClientState.Idle && _clientState.value !is ClientState.Error) {
                                        Log.w(TAG, "Unexpected Disconnection. Setting Disconnected.")
                                        _clientState.value = ClientState.Disconnected
                                    }
                                }

                                is ConnectionEvent.Error -> {
                                    // Handle transport errors
                                    if (_clientState.value !is ClientState.Idle) {
                                        _clientState.value = ClientState.Error(event.message)
                                    }
                                }

                                else -> {} // Ignore Connected, Sent, etc.
                            }
                        }
                    }
                } catch (e: TimeoutCancellationException) {
                    Log.e(TAG, "Connection process timed out.")
                    _clientState.value =
                        ClientState.Error("Connection timed out. Please try again.", canRetry = true)
                } catch (e: CancellationException) {
                    Log.e(TAG, "RemoteClientNetworkRepository: Cancelled during connection", e)
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Unexpected error during connection: ${e.message}")
                    _clientState.value = ClientState.Error("Unexpected Error: ${e.message}")
                } finally {
                    cleanupResources()
                }
            }

        override suspend fun disconnect() {
            _clientState.value = ClientState.Idle
            cleanupResources()
        }

        override suspend fun sendToServer(message: ClientMessage): Boolean {
            val encodedMessage = NetworkJson.encodeToString<ClientMessage>(message)
            Log.d(TAG, "RemoteClientRepository: Sending encoded message: $encodedMessage")
            return socketClient.sendToServer(encodedMessage)
        }

        //region Helper Functions
        private suspend fun performDiscovery(gameCode: String): NsdServiceInfo? {
            networkDiscovery.startDiscovery(targetGameCode = gameCode)

            // Wait for either Discovering State (Success start) or Failed State
            val discoveryState =
                withTimeout(TIMEOUT_MS) {
                    networkDiscovery.discoveryProcessState.first {
                        it is NsdDiscoveryState.Discovering || it is NsdDiscoveryState.Failed
                    }
                }

            if (discoveryState is NsdDiscoveryState.Failed) {
                _clientState.value = ClientState.Error(discoveryState.error)
                return null
            }

            _clientState.value = ClientState.Discovering

            // Wait for the actual Service Found event
            Log.d(TAG, "Performed discovery: getting service info from events")
            return withTimeout(TIMEOUT_MS) {
                networkDiscovery.discoveredServiceEvent
                    .filterIsInstance<NsdDiscoveryEvent.Found>()
                    .first()
                    .serviceInfo
            }
        }

        private suspend fun performResolution(
            serviceInfo: NsdServiceInfo,
            gameCode: String,
        ): Pair<String, Int>? {
            _clientState.value = ClientState.Resolving
            networkResolution.resolveServiceWithGameCode(serviceInfo, gameCode)

            val resolutionState =
                withTimeout(TIMEOUT_MS) {
                    networkResolution.resolutionState.first {
                        it is NsdResolutionState.Success || it is NsdResolutionState.Failed
                    }
                }

            // We stop discovery now that we have (or failed to get) the IP
            networkDiscovery.stopDiscovery()

            return when (resolutionState) {
                is NsdResolutionState.Success -> {
                    _clientState.value = ClientState.Connecting
                    resolutionState.host to resolutionState.port
                }

                is NsdResolutionState.Failed -> {
                    _clientState.value = ClientState.Error(resolutionState.error)
                    null
                }

                else -> null // Should be unreachable given the filter above
            }
        }

        private suspend fun performSocketConnection(
            host: String,
            port: Int,
        ) = coroutineScope {
            launch(ioDispatcher) { socketClient.startSession(host, port) }

            val event =
                withTimeout(TIMEOUT_MS) {
                    socketClient.connectionEvents
                        .first { it is ConnectionEvent.Connected || it is ConnectionEvent.Error }
                }
            Log.d(TAG, "performSocketConnection: event ($event)")
            when (event) {
                is ConnectionEvent.Connected -> {
                    _clientState.value = ClientState.Connected
                }

                is ConnectionEvent.Error -> {
                    _clientState.value = ClientState.Error(event.message)
                }

                else -> { // Ignore Disconnected events during startup
                }
            }
        }

        private suspend fun cleanupResources() {
            networkDiscovery.stopDiscovery()
            socketClient.disconnect()
        }

        /**
         * Light-weight check to see if a raw JSON string corresponds to a graceful exit message.
         * This avoids full deserialization overhead in the monitoring loop.
         */
        private fun isGracefulExitSignal(json: String): Boolean {
            // Quick string check is faster and safer here than full decode
            return json.contains("LobbyClosed") || json.contains("YouWereKicked")
        }
        //endregion

        companion object {
            const val TIMEOUT_MS = 15_000L
        }
    }
