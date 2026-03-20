package com.mostafa.impostle.domain.e2e.fakes

import com.mostafa.impostle.domain.model.ClientMessage
import com.mostafa.impostle.domain.model.ClientState
import com.mostafa.impostle.domain.model.ServerMessage
import com.mostafa.impostle.domain.repository.ClientNetworkRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A fake [ClientNetworkRepository] for E2E tests.
 *
 * Instead of using NSD discovery and Ktor sockets, it registers with
 * [InMemoryNetworkRouter] on [connect] and routes all outgoing messages through
 * the router. The router feeds incoming server messages into [_incomingMessages].
 */
class FakeClientNetworkRepository(
    private val router: InMemoryNetworkRouter,
    private val gameCode: String,
    private val clientId: String,
) : ClientNetworkRepository {
    @Suppress("ktlint:standard:backing-property-naming")
    val _clientState = MutableStateFlow<ClientState>(ClientState.Idle)
    override val clientState: StateFlow<ClientState> = _clientState.asStateFlow()

    /** Router feeds incoming server messages into this flow. */
    @Suppress("ktlint:standard:backing-property-naming")
    val _incomingMessages =
        MutableSharedFlow<Pair<String, ServerMessage>>(extraBufferCapacity = 64)
    override val incomingMessages: Flow<Pair<String, ServerMessage>> =
        _incomingMessages.asSharedFlow()

    // Not consumed by GameClient — only used in production for debugging.
    override val outGoingMessages: Flow<Pair<String, ClientMessage>> = MutableSharedFlow()

    override suspend fun connect(gameCode: String) {
        router.registerClient(gameCode, clientId, this)
        _clientState.value = ClientState.Connected
    }

    override suspend fun disconnect() {
        _clientState.value = ClientState.Disconnected
        router.unregisterClient(gameCode, clientId)
    }

    override suspend fun sendToServer(message: ClientMessage): Boolean = router.routeToServer(gameCode, clientId, message)
}
