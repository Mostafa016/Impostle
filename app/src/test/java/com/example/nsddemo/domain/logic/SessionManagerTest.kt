package com.example.nsddemo.domain.logic

import com.example.nsddemo.domain.model.ClientMessage
import com.example.nsddemo.domain.model.Envelope
import com.example.nsddemo.domain.model.GamePhase
import com.example.nsddemo.domain.model.GameStateTransition
import com.example.nsddemo.domain.model.NewGameData
import com.example.nsddemo.domain.model.NewPlayerColors
import com.example.nsddemo.domain.model.Player
import com.example.nsddemo.domain.model.RoundData
import com.example.nsddemo.domain.model.ServerMessage
import com.example.nsddemo.domain.model.SystemEvent
import com.example.nsddemo.domain.util.GameFlowRegistry
import com.example.nsddemo.domain.util.PlayerCountLimits
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SessionManagerTest {

    private lateinit var sessionManager: SessionManager

    // Helper Data
    private val hostId = "host_1"
    private val playerId = "p2"
    private val player = Player("Bob", "Blue", playerId, isConnected = true)

    // Base data with one player already inside
    private val baseData = NewGameData(
        hostId = hostId,
        players = mapOf(playerId to player)
    )

    @Before
    fun setUp() {
        // Mock Singletons
        mockkObject(GameFlowRegistry)
        mockkObject(ColorAllocator)

        // Default Mocks
        every { ColorAllocator.assignColor(any()) } returns NewPlayerColors.Red
        every { GameFlowRegistry.getValidPhasesFor(any()) } returns setOf(
            GamePhase.Lobby,
            GamePhase.Idle
        )

        sessionManager = SessionManager()
    }

    @After
    fun tearDown() {
        unmockkObject(GameFlowRegistry)
        unmockkObject(ColorAllocator)
    }

    // ==========================================
    // 1. NEW PLAYER JOINING
    // ==========================================

    @Test
    fun `GIVEN Lobby and Space Available WHEN Register New Player THEN Success`() {
        val msg = ClientMessage.RegisterPlayer("Alice", "new_p3")
        val result = sessionManager.registerPlayer(baseData, GamePhase.Lobby, msg)

        assertTrue(result is GameStateTransition.Valid)
        val valid = result as GameStateTransition.Valid
        assertEquals(2, valid.newGameData.players.size)
        assertTrue(valid.envelopes[0] is Envelope.Unicast)
        assertTrue(valid.envelopes[1] is Envelope.Broadcast)
    }

    @Test
    fun `GIVEN Idle Phase WHEN Register New Player THEN Success (Strict Mode)`() {
        // Arrange: Game just started (Idle)
        val emptyData = NewGameData(hostId = "", players = emptyMap())
        val msg = ClientMessage.RegisterPlayer("Host", hostId)

        val result = sessionManager.registerPlayer(emptyData, GamePhase.Idle, msg)

        assertTrue(result is GameStateTransition.Valid)
        val valid = result as GameStateTransition.Valid
        assertEquals(1, valid.newGameData.players.size)
    }

    @Test
    fun `GIVEN InRound Phase WHEN Register New Player THEN Invalid`() {
        val msg = ClientMessage.RegisterPlayer("Alice", "new_p3")
        every { GameFlowRegistry.getValidPhasesFor(msg) } returns setOf(
            GamePhase.Lobby,
            GamePhase.Idle
        )

        val result = sessionManager.registerPlayer(baseData, GamePhase.InRound, msg)

        assertTrue(result is GameStateTransition.Invalid)
        val invalid = result as GameStateTransition.Invalid
        assertTrue(invalid.reason.contains("already started"))
        assertEquals(
            ServerMessage.GameAlreadyStarted,
            (invalid.envelopes.first() as Envelope.Unicast).message
        )
    }

    @Test
    fun `GIVEN Lobby Full WHEN Register New Player THEN Invalid`() {
        val msg = ClientMessage.RegisterPlayer("Overflow", "p_overflow")
        val fullMap = (1..PlayerCountLimits.MAX_PLAYERS).associate { "p$it" to player }
        val fullData = baseData.copy(players = fullMap)

        val result = sessionManager.registerPlayer(fullData, GamePhase.Lobby, msg)

        assertTrue(result is GameStateTransition.Invalid)
        assertEquals(
            ServerMessage.GameFull,
            ((result as GameStateTransition.Invalid).envelopes.first() as Envelope.Unicast).message
        )
    }

    // ==========================================
    // 2. RECONNECTION & AUTO-RESUME
    // ==========================================

    @Test
    fun `GIVEN Disconnected Player WHEN Register Same ID THEN Success (Reconnection)`() {
        val offlinePlayer = player.copy(isConnected = false)
        val data = baseData.copy(players = mapOf(playerId to offlinePlayer))
        val msg = ClientMessage.RegisterPlayer("Bob Updated", playerId)

        val result = sessionManager.registerPlayer(data, GamePhase.InRound, msg)

        assertTrue(result is GameStateTransition.Valid)
        val valid = result as GameStateTransition.Valid

        // Check Player Status
        assertTrue(valid.newGameData.players[playerId]!!.isConnected)

        // Check Messages
        val messages = valid.envelopes.map {
            when (it) {
                is Envelope.Broadcast -> it.message
                is Envelope.Unicast -> it.message
            }
        }
        assertTrue(messages.any { it is ServerMessage.PlayerReconnected })
        assertTrue(messages.any { it is ServerMessage.ReconnectionFullStateSync })
    }

    @Test
    fun `GIVEN Connected Player WHEN Register Same ID THEN Invalid (Already Connected)`() {
        val msg = ClientMessage.RegisterPlayer("Bob", playerId)
        val result = sessionManager.registerPlayer(baseData, GamePhase.Lobby, msg)

        assertTrue(result is GameStateTransition.Invalid)
        assertTrue((result as GameStateTransition.Invalid).reason.contains("already connected"))
    }

    @Test
    fun `GIVEN Paused Game WHEN All Players Reconnected THEN Auto-Resume`() {
        // Arrange: Game paused because Bob disconnected from InRound
        val pausedData = baseData.copy(
            players = mapOf(
                playerId to player.copy(isConnected = false), // Bob offline
                hostId to Player("Host", "Red", hostId, isConnected = true) // Host online
            ),
            phaseBeforePause = GamePhase.InRound
        )
        val msg = ClientMessage.RegisterPlayer("Bob", playerId)

        // Act: Bob reconnects
        val result = sessionManager.registerPlayer(pausedData, GamePhase.Paused, msg)

        // Assert
        val valid = result as GameStateTransition.Valid

        // 1. Phase restored to InRound
        assertEquals(GamePhase.InRound, valid.newPhase)

        // 2. phaseBeforePause cleared
        assertNull(valid.newGameData.phaseBeforePause)

        // 3. GameResumed broadcast sent
        val messages = valid.envelopes.map {
            when (it) {
                is Envelope.Broadcast -> it.message
                is Envelope.Unicast -> it.message
            }
        }
        assertTrue("Should broadcast GameResumed", messages.contains(ServerMessage.GameResumed))
    }

    @Test
    fun `GIVEN Paused Game WHEN One of Many Reconnects THEN Stay Paused`() {
        // Arrange: 2 players offline (Bob & Charlie). Host online.
        val pausedData = baseData.copy(
            players = mapOf(
                playerId to player.copy(isConnected = false), // Bob
                "p3" to Player("Charlie", "Green", "p3", isConnected = false), // Charlie
                hostId to Player("Host", "Red", hostId, isConnected = true)
            ),
            phaseBeforePause = GamePhase.InRound
        )
        val msg = ClientMessage.RegisterPlayer("Bob", playerId)

        // Act: Bob reconnects (Charlie still missing)
        val result = sessionManager.registerPlayer(pausedData, GamePhase.Paused, msg)

        // Assert
        val valid = result as GameStateTransition.Valid

        // 1. Phase stays Paused
        assertEquals(GamePhase.Paused, valid.newPhase)

        // 2. phaseBeforePause persisted
        assertEquals(GamePhase.InRound, valid.newGameData.phaseBeforePause)

        // 3. NO GameResumed message
        val messages = valid.envelopes.map {
            when (it) {
                is Envelope.Broadcast -> it.message
                is Envelope.Unicast -> it.message
            }
        }
        assertFalse(messages.contains(ServerMessage.GameResumed))
    }

    @Test
    fun `GIVEN Reconnection WHEN RoundData is Question THEN Syncs Sliced Data`() {
        // Arrange: Round Data has 5 pairs, current index 0. Sync should only send index 0.
        val roundData = RoundData.QuestionRoundData(
            roundPairs = listOf("p1" to "p2", "p2" to "p3"),
            currentPairIndex = 0
        )
        val gameData = baseData.copy(
            players = mapOf(playerId to player.copy(isConnected = false)),
            roundData = roundData
        )
        val msg = ClientMessage.RegisterPlayer("Bob", playerId)

        val result = sessionManager.registerPlayer(gameData, GamePhase.InRound, msg)
        val valid = result as GameStateTransition.Valid

        // Verify Round Data Slicing in Sync Message
        val syncMsg =
            (valid.envelopes.find { it is Envelope.Unicast }!! as Envelope.Unicast).message as ServerMessage.ReconnectionFullStateSync
        val syncRound = syncMsg.data.roundData as RoundData.QuestionRoundData

        assertEquals(1, syncRound.roundPairs.size) // Only 1 pair sent
    }

    // ==========================================
    // 3. DISCONNECTION
    // ==========================================

    @Test
    fun `GIVEN Lobby Phase WHEN Player Disconnects THEN Remove Player`() {
        val event = SystemEvent.PlayerDisconnected(playerId)
        val result = sessionManager.handleSystemEvent(baseData, GamePhase.Lobby, event)

        val valid = result as GameStateTransition.Valid
        assertFalse(valid.newGameData.players.containsKey(playerId)) // Deleted
        assertTrue((valid.envelopes.first() as Envelope.Broadcast).message is ServerMessage.PlayerList)
    }

    @Test
    fun `GIVEN Idle Phase WHEN Player Disconnects THEN Remove Player (Strict Mode)`() {
        // Logic: Idle is not Active/Paused, so falls to else -> Delete
        val event = SystemEvent.PlayerDisconnected(playerId)
        val result = sessionManager.handleSystemEvent(baseData, GamePhase.Idle, event)

        val valid = result as GameStateTransition.Valid
        assertFalse(valid.newGameData.players.containsKey(playerId)) // Deleted
    }

    @Test
    fun `GIVEN InRound (Active) WHEN Player Disconnects THEN Pause Game`() {
        val event = SystemEvent.PlayerDisconnected(playerId)
        val result = sessionManager.handleSystemEvent(baseData, GamePhase.InRound, event)

        val valid = result as GameStateTransition.Valid

        // 1. Phase -> Paused
        assertEquals(GamePhase.Paused, valid.newPhase)

        // 2. Saved previous phase
        assertEquals(GamePhase.InRound, valid.newGameData.phaseBeforePause)

        // 3. Player kept but offline
        assertTrue(valid.newGameData.players.containsKey(playerId))
        assertFalse(valid.newGameData.players[playerId]!!.isConnected)

        // 4. Broadcast Disconnect (Client triggers pause UI)
        val msg = (valid.envelopes.first() as Envelope.Broadcast).message
        assertTrue(msg is ServerMessage.PlayerDisconnected)
    }

    @Test
    fun `GIVEN Already Paused WHEN Another Player Disconnects THEN Stay Paused & Preserve State`() {
        // Arrange: Already paused because Host disconnected previously
        val pausedData = baseData.copy(
            players = mapOf(
                hostId to Player("Host", "Red", hostId, isConnected = false),
                playerId to player // Bob is currently online
            ),
            phaseBeforePause = GamePhase.GameVoting // Was voting before pause
        )

        val event = SystemEvent.PlayerDisconnected(playerId) // Bob disconnects too

        // Act
        val result = sessionManager.handleSystemEvent(pausedData, GamePhase.Paused, event)
        val valid = result as GameStateTransition.Valid

        // Assert
        assertEquals(GamePhase.Paused, valid.newPhase)

        // CRITICAL: Ensure we didn't overwrite 'GameVoting' with 'Paused'
        assertEquals(GamePhase.GameVoting, valid.newGameData.phaseBeforePause)

        assertFalse(valid.newGameData.players[playerId]!!.isConnected)
    }

    @Test
    fun `GIVEN Unknown Player WHEN Disconnects THEN Ignore`() {
        val event = SystemEvent.PlayerDisconnected("unknown_id")
        val result = sessionManager.handleSystemEvent(baseData, GamePhase.InRound, event)

        assertTrue(result is GameStateTransition.Invalid)
    }
}