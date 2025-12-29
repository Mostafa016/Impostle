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
import com.example.nsddemo.data.util.ClientState
import com.example.nsddemo.domain.repository.ClientNetworkRepository
import com.example.nsddemo.domain.util.ClientMessage
import com.example.nsddemo.domain.util.NetworkJson
import com.example.nsddemo.domain.util.ServerMessage
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.encodeToString
import java.io.IOException
import javax.inject.Inject

class KtorClientNetworkRepository @Inject constructor(
    private val networkDiscovery: NetworkDiscovery,
    private val networkResolution: NetworkResolution,
    private val socketClient: SocketClient
) : ClientNetworkRepository {

    private val _clientState = MutableStateFlow<ClientState>(ClientState.Idle)
    override val clientState = _clientState.asStateFlow()

    // Mapping Raw Socket events to Domain Sealed Classes
    override val incomingMessages: Flow<Pair<String, ClientMessage>> =
        socketClient.messageEvents.filterIsInstance(MessageEvent.Received::class)
            .mapNotNull { event ->
                try {
                    event.clientId to NetworkJson.decodeFromString<ClientMessage>(event.data)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse message: ${e.message}")
                    null // Drop bad packet, keep connection alive
                }
            }

    override val outGoingMessages: Flow<Pair<String, ServerMessage>> =
        socketClient.messageEvents.filterIsInstance(MessageEvent.Sent::class)
            .map { it.clientId to NetworkJson.decodeFromString<ServerMessage>(it.data) }

    override suspend fun connect(gameCode: String) = coroutineScope {
        try {
            // 1. Discovery Phase
            val serviceInfo = performDiscovery(gameCode) ?: return@coroutineScope

            // 2. Resolution Phase
            val (host, port) = performResolution(serviceInfo, gameCode) ?: return@coroutineScope

            // 3. Connection Phase
            performSocketConnection(host, port)

        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Connection process timed out.")
            _clientState.value =
                ClientState.Error("Connection timed out. Please try again.", canRetry = true)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during connection: ${e.message}")
            _clientState.value = ClientState.Error("Unexpected Error: ${e.message}")
        } finally {
            cleanupResources()
        }
    }

    override suspend fun disconnect() {
        cleanupResources()
        _clientState.value = ClientState.Disconnected
    }

    override suspend fun sendToServer(message: ClientMessage): Boolean =
        socketClient.sendToServer(NetworkJson.encodeToString<ClientMessage>(message))

    //region Helper Functions
    private suspend fun performDiscovery(gameCode: String): NsdServiceInfo? {
        networkDiscovery.startDiscovery(targetGameCode = gameCode)

        // Wait for either Discovering State (Success start) or Failed State
        val discoveryState = withTimeout(TIMEOUT_MS) {
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
        return withTimeout(TIMEOUT_MS) {
            networkDiscovery.discoveredServiceEvent
                .filterIsInstance<NsdDiscoveryEvent.Found>()
                .first()
                .serviceInfo
        }
    }

    private suspend fun performResolution(
        serviceInfo: NsdServiceInfo,
        gameCode: String
    ): Pair<String, Int>? {
        _clientState.value = ClientState.Resolving
        networkResolution.resolveServiceWithGameCode(serviceInfo, gameCode)

        val resolutionState = withTimeout(TIMEOUT_MS) {
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

    private suspend fun performSocketConnection(host: String, port: Int) = coroutineScope {
        launch { socketClient.startSession(host, port) }

        withTimeout(TIMEOUT_MS) {
            val event = socketClient.connectionEvents
                .first { it is ConnectionEvent.Connected || it is ConnectionEvent.Error }

            when (event) {
                is ConnectionEvent.Connected -> {
                    _clientState.value = ClientState.Connected
                }

                is ConnectionEvent.Error -> {
                    throw IOException("Handshake Failed: ${event.message}")
                }

                else -> { /* Ignore Disconnected events during startup */
                }
            }
        }
    }

    private suspend fun cleanupResources() {
        networkDiscovery.stopDiscovery()
        socketClient.disconnect()
    }
    //endregion

    companion object {
        const val TIMEOUT_MS = 10_000L
    }
}