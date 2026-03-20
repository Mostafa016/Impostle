package com.mostafa.impostle.domain.e2e.fakes

import com.mostafa.impostle.domain.model.ClientMessage
import com.mostafa.impostle.domain.model.ClientState
import com.mostafa.impostle.domain.model.PlayerConnectionEvent
import com.mostafa.impostle.domain.model.ServerMessage

/**
 * The virtual "Wi-Fi router" for E2E tests.
 *
 * Acts as the in-memory message bus between [FakeServerNetworkRepository] and
 * one or more [FakeClientNetworkRepository] instances. All domain messages
 * ([ClientMessage] / [ServerMessage]) are passed directly as Kotlin objects —
 * no serialisation, no sockets, no NSD.
 *
 * Thread-safety: all access is via the coroutine dispatcher used in [runTest]
 * (single-threaded by default), so no explicit synchronisation is needed.
 */
class InMemoryNetworkRouter {
    // gameCode → server repo
    private val servers = mutableMapOf<String, FakeServerNetworkRepository>()

    // gameCode → (clientId → client repo)
    private val clients = mutableMapOf<String, MutableMap<String, FakeClientNetworkRepository>>()

    // ─── Server registration ────────────────────────────────────────────────

    fun registerServer(
        gameCode: String,
        repo: FakeServerNetworkRepository,
    ) {
        servers[gameCode] = repo
        clients.getOrPut(gameCode) { mutableMapOf() }
    }

    fun unregisterServer(gameCode: String) {
        servers.remove(gameCode)
        clients.remove(gameCode)
    }

    // ─── Client registration ────────────────────────────────────────────────

    fun registerClient(
        gameCode: String,
        clientId: String,
        repo: FakeClientNetworkRepository,
    ) {
        clients.getOrPut(gameCode) { mutableMapOf() }[clientId] = repo
        // Notify the server that a new TCP connection arrived
        servers[gameCode]?._playerConnectionEvents?.tryEmit(
            PlayerConnectionEvent.PlayerConnected(clientId, clientId),
        )
    }

    fun unregisterClient(
        gameCode: String,
        clientId: String,
    ) {
        clients[gameCode]?.remove(clientId)
    }

    // ─── Message Routing ────────────────────────────────────────────────────

    /** Called by [FakeClientNetworkRepository.sendToServer]. Routes Client → Server. */
    fun routeToServer(
        gameCode: String,
        clientId: String,
        message: ClientMessage,
    ): Boolean {
        val server = servers[gameCode] ?: return false
        server._incomingMessages.tryEmit(clientId to message)
        return true
    }

    /** Called by [FakeServerNetworkRepository.sendToPlayer]. Routes Server → single client. */
    suspend fun deliverToClient(
        gameCode: String,
        clientId: String,
        message: ServerMessage,
    ) {
        clients[gameCode]?.get(clientId)?._incomingMessages?.emit("server" to message)
    }

    /** Called by [FakeServerNetworkRepository.sendToAllPlayers]. Routes Server → all clients. */
    suspend fun broadcastToClients(
        gameCode: String,
        message: ServerMessage,
    ) {
        clients[gameCode]?.values?.forEach { clientRepo ->
            clientRepo._incomingMessages.emit("server" to message)
        }
    }

    // ─── Fault Injection ────────────────────────────────────────────────────

    /**
     * Simulates a Wi-Fi disconnection for [clientId]:
     *  1. Notifies the **server's** `playerConnectionEvents` flow so [GameServer.processActions]
     *     triggers [SessionManager.handleSystemEvent(PlayerDisconnected)] → Pause.
     *  2. Sets the **client's** [ClientState] to [ClientState.Disconnected].
     *  3. Removes the client from the routing table so future broadcasts skip it.
     */
    fun dropConnection(
        gameCode: String,
        clientId: String,
    ) {
        // 1. Notify server
        servers[gameCode]?._playerConnectionEvents?.tryEmit(
            PlayerConnectionEvent.PlayerDisconnected(clientId),
        )
        // 2. Notify client
        clients[gameCode]?.get(clientId)?._clientState?.value = ClientState.Disconnected
        // 3. Remove from table
        clients[gameCode]?.remove(clientId)
    }

    /**
     * Called by [FakeServerNetworkRepository.disconnectPlayer] (i.e. during a kick).
     * Only clears the client side — the server side disconnect event has already been emitted
     * by [FakeServerNetworkRepository.disconnectPlayer] before calling this.
     */
    fun dropClientSide(
        gameCode: String,
        clientId: String,
    ) {
        clients[gameCode]?.get(clientId)?._clientState?.value = ClientState.Disconnected
        clients[gameCode]?.remove(clientId)
    }
}
