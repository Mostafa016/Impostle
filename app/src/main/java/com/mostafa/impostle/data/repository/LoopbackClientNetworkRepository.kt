package com.mostafa.impostle.data.repository

import com.mostafa.impostle.data.local.network.LoopbackDataSource
import com.mostafa.impostle.domain.model.ClientMessage
import com.mostafa.impostle.domain.model.ClientState
import com.mostafa.impostle.domain.model.ServerMessage
import com.mostafa.impostle.domain.repository.ClientNetworkRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

class LoopbackClientNetworkRepository
    @Inject
    constructor(
        private val loopbackDataSource: LoopbackDataSource,
    ) : ClientNetworkRepository {
        private val _clientState = MutableStateFlow<ClientState>(ClientState.Idle)
        override val clientState = _clientState.asStateFlow()

        override val incomingMessages: Flow<Pair<String, ServerMessage>> =
            loopbackDataSource.serverToClient // TODO: Add a check to map the LOCAL_HOST_CLIENT_ID to the current player id in messages

        override val outGoingMessages: Flow<Pair<String, ClientMessage>> =
            loopbackDataSource.clientToServer

        override suspend fun connect(gameCode: String) {
            _clientState.value = ClientState.Connected
        }

        override suspend fun disconnect() {
            _clientState.value = ClientState.Disconnected
        }

        override suspend fun sendToServer(message: ClientMessage): Boolean {
            loopbackDataSource.clientToServer.emit(LoopbackDataSource.LOCAL_HOST_CLIENT_ID to message)
            return true
        }
    }
