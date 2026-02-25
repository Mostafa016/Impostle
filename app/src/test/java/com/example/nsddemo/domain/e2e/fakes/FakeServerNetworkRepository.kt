package com.example.nsddemo.domain.e2e.fakes

import com.example.nsddemo.domain.model.ClientMessage
import com.example.nsddemo.domain.model.PlayerConnectionEvent
import com.example.nsddemo.domain.model.ServerMessage
import com.example.nsddemo.domain.model.ServerState
import com.example.nsddemo.domain.repository.ServerNetworkRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A fake [ServerNetworkRepository] for E2E tests.
 *
 * Instead of opening a TCP port via Ktor, it registers itself with
 * [InMemoryNetworkRouter] and exposes internal [MutableSharedFlow]s that the
 * router writes into directly — simulating the arrival of client messages and
 * player connection events.
 */
class FakeServerNetworkRepository(
    private val router: InMemoryNetworkRouter,
    private val gameCode: String,
) : ServerNetworkRepository {

    private val _serverState = MutableStateFlow<ServerState>(ServerState.Idle)
    override val serverState: StateFlow<ServerState> = _serverState.asStateFlow()

    /** Router feeds incoming client messages into this flow. */
    val _incomingMessages = MutableSharedFlow<Pair<String, ClientMessage>>(extraBufferCapacity = 64)
    override val incomingMessages: Flow<Pair<String, ClientMessage>> =
        _incomingMessages.asSharedFlow()

    /** Router feeds connect/disconnect events into this flow. */
    val _connectionEvents = MutableSharedFlow<PlayerConnectionEvent>(extraBufferCapacity = 64)
    override val playerConnectionEvents: Flow<PlayerConnectionEvent> =
        _connectionEvents.asSharedFlow()

    // Not used in E2E tests – the router intercepts outgoing messages at the send* call sites.
    override val outGoingMessages: Flow<Pair<String, ServerMessage>> = MutableSharedFlow()

    override suspend fun start(gameCode: String) {
        router.registerServer(gameCode, this)
        _serverState.value = ServerState.Running(port = 0, gameCode = gameCode)
    }

    override suspend fun sendToPlayer(playerId: String, message: ServerMessage) {
        router.deliverToClient(gameCode, playerId, message)
    }

    override suspend fun sendToAllPlayers(message: ServerMessage) {
        router.broadcastToClients(gameCode, message)
    }

    /**
     * Called by [GameServer] when it wants to force-disconnect a player (kick).
     * Emits a [PlayerConnectionEvent.PlayerDisconnected] back into the server's own event
     * flow so [GameServer.processActions] is notified, and also clears the client side.
     */
    override fun disconnectPlayer(playerId: String) {
        _connectionEvents.tryEmit(PlayerConnectionEvent.PlayerDisconnected(playerId))
        router.dropClientSide(gameCode, playerId)
    }

    override fun cancelAdvertising() { /* no-op in tests */
    }

    override suspend fun stop() {
        _serverState.value = ServerState.Idle
        router.unregisterServer(gameCode)
    }
}
