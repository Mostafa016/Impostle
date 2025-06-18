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
import com.example.nsddemo.domain.util.ClientMessage
import com.example.nsddemo.domain.util.NetworkJson
import com.example.nsddemo.domain.util.ServerMessage
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import javax.inject.Inject

class KtorClientNetworkRepository @Inject constructor(
    private val networkDiscovery: NetworkDiscovery,
    private val networkResolution: NetworkResolution,
    private val socketClient: SocketClient
) : ClientNetworkRepository {
    private val _clientState = MutableStateFlow<ClientState>(ClientState.Idle)
    override val clientState: Flow<ClientState> = _clientState

    override val incomingMessages: Flow<Pair<String, ClientMessage>> =
        socketClient.messageEvents.filterIsInstance(MessageEvent.Received::class)
            .map { it.clientId to NetworkJson.decodeFromString<ClientMessage>(it.data) }
    override val outGoingMessages: Flow<Pair<String, ServerMessage>> =
        socketClient.messageEvents.filterIsInstance(MessageEvent.Sent::class)
            .map { it.clientId to NetworkJson.decodeFromString<ServerMessage>(it.data) }

    override suspend fun connect(gameCode: String) = coroutineScope {
        val isDiscoverySuccessful = startDiscoveryWithGameCode(gameCode)
        if (!isDiscoverySuccessful) return@coroutineScope

        val serviceInfo =
            networkDiscovery.discoveredServiceEvent.filterIsInstance(NsdDiscoveryEvent.Found::class)
                .first().serviceInfo
        val isResolutionSuccessful =
            startServiceResolutionWithGameCode(
                serviceInfo = serviceInfo,
                gameCode = gameCode
            )
        networkDiscovery.stopDiscovery()
        if (!isResolutionSuccessful) return@coroutineScope

        val (serverHost, serverPort) = networkResolution.resolutionState.value as NsdResolutionState.Success
        val isConnectionSuccessful = socketClient.connect(host = serverHost, port = serverPort)
        if (!isConnectionSuccessful) {
            val clientConnectionErrorState =
                socketClient.connectionEvents.first() as ConnectionEvent.Error
            _clientState.value = ClientState.Error(clientConnectionErrorState.message)
            return@coroutineScope
        }

        _clientState.value = ClientState.Connected
    }


    override suspend fun disconnect() {
        networkDiscovery.stopDiscovery()
        socketClient.disconnect()
        _clientState.value = ClientState.Disconnected
    }

    override suspend fun sendToServer(message: ClientMessage): Boolean =
        socketClient.sendToServer(NetworkJson.encodeToString<ClientMessage>(message))

    //region connect() Helpers
    private suspend fun startDiscoveryWithGameCode(gameCode: String): Boolean {
        networkDiscovery.startDiscovery(targetGameCode = gameCode)
        val networkDiscoveryState =
            networkDiscovery.discoveryProcessState.first { it is NsdDiscoveryState.Discovering || it is NsdDiscoveryState.Failed }
        when (networkDiscoveryState) {
            is NsdDiscoveryState.Discovering -> {
                _clientState.value = ClientState.Discovering
            }

            is NsdDiscoveryState.Failed -> {
                _clientState.value = ClientState.Error(networkDiscoveryState.error)
                return false
            }

            else -> {
                Log.wtf(
                    TAG,
                    "Unexpected discovery state: $networkDiscoveryState when connecting to server"
                )
                return false
            }
        }
        return true
    }

    private suspend fun startServiceResolutionWithGameCode(
        serviceInfo: NsdServiceInfo,
        gameCode: String
    ): Boolean {
        _clientState.value = ClientState.Resolving
        networkResolution.resolveServiceWithGameCode(serviceInfo = serviceInfo, gameCode = gameCode)
        val networkResolutionState =
            networkResolution.resolutionState.first { it is NsdResolutionState.Success || it is NsdResolutionState.Failed }
        when (networkResolutionState) {
            is NsdResolutionState.Success -> {
                _clientState.value = ClientState.Connecting
            }

            is NsdResolutionState.Failed -> {
                _clientState.value = ClientState.Error(networkResolutionState.error)
                return false
            }

            else -> {
                Log.wtf(
                    TAG,
                    "Unexpected resolution state: $networkResolutionState when connecting to server"
                )
                return false
            }
        }
        return true
    }
    //endregion
}