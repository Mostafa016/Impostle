package com.example.nsddemo.domain.logic

import android.util.Log
import com.example.nsddemo.domain.model.ClientMessage
import com.example.nsddemo.domain.model.Envelope
import com.example.nsddemo.domain.model.GameData
import com.example.nsddemo.domain.model.GamePhase
import com.example.nsddemo.domain.model.GameStateTransition
import com.example.nsddemo.domain.model.NewPlayerColors
import com.example.nsddemo.domain.model.Player
import com.example.nsddemo.domain.model.RoundData
import com.example.nsddemo.domain.model.ServerMessage
import com.example.nsddemo.domain.model.SystemEvent
import com.example.nsddemo.domain.strategy.GameModeStrategy
import com.example.nsddemo.domain.util.GameFlowRegistry
import com.example.nsddemo.domain.util.PlayerCountLimits
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
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
    private val baseData =
        GameData(
            hostId = hostId,
            players = mapOf(playerId to player),
        )

    @Before
    fun setUp() {
        // Mock Singletons
        mockkObject(GameFlowRegistry)
        mockkObject(ColorAllocator)
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } answers {
            // Print the error if the test fails so we see why
            println("ERROR: ${secondArg<String>()} Exception: ${thirdArg<Throwable>()}")
            0
        }
        every { Log.w(any(), any<Throwable>()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.i(any(), any()) } returns 0
        // Default Mocks
        every { ColorAllocator.assignColor(any()) } returns NewPlayerColors.Red
        every { GameFlowRegistry.getValidPhasesFor(any()) } returns
            setOf(
                GamePhase.Lobby,
                GamePhase.Idle,
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
        val emptyData = GameData(hostId = "", players = emptyMap())
        val msg = ClientMessage.RegisterPlayer("Host", hostId)

        val result = sessionManager.registerPlayer(emptyData, GamePhase.Idle, msg)

        assertTrue(result is GameStateTransition.Valid)
        val valid = result as GameStateTransition.Valid
        assertEquals(1, valid.newGameData.players.size)
    }

    @Test
    fun `GIVEN InRound Phase WHEN Register New Player THEN Invalid`() {
        val msg = ClientMessage.RegisterPlayer("Alice", "new_p3")
        every { GameFlowRegistry.getValidPhasesFor(msg) } returns
            setOf(
                GamePhase.Lobby,
                GamePhase.Idle,
            )

        val result = sessionManager.registerPlayer(baseData, GamePhase.InRound, msg)

        assertTrue(result is GameStateTransition.Invalid)
        val invalid = result as GameStateTransition.Invalid
        assertTrue(invalid.reason.contains("already started"))
        assertEquals(
            ServerMessage.GameAlreadyStarted,
            (invalid.envelopes.first() as Envelope.Unicast).message,
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
            ((result as GameStateTransition.Invalid).envelopes.first() as Envelope.Unicast).message,
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
        val messages =
            valid.envelopes.map {
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
        val pausedData =
            baseData.copy(
                players =
                    mapOf(
                        playerId to player.copy(isConnected = false), // Bob offline
                        hostId to Player("Host", "Red", hostId, isConnected = true), // Host online
                    ),
                phaseAfterPause = GamePhase.InRound,
            )
        val msg = ClientMessage.RegisterPlayer("Bob", playerId)

        // Act: Bob reconnects
        val result = sessionManager.registerPlayer(pausedData, GamePhase.Paused, msg)

        // Assert
        val valid = result as GameStateTransition.Valid

        // 1. Phase restored to InRound
        assertEquals(GamePhase.InRound, valid.newPhase)

        // 2. phaseBeforePause cleared
        assertNull(valid.newGameData.phaseAfterPause)

        // 3. GameResumed broadcast sent
        val messages =
            valid.envelopes.map {
                when (it) {
                    is Envelope.Broadcast -> it.message
                    is Envelope.Unicast -> it.message
                }
            }
        assertTrue(
            "Should broadcast GameResumed",
            messages.contains(ServerMessage.GameResumed(GamePhase.InRound)),
        )
    }

    @Test
    fun `GIVEN Paused Game WHEN One of Many Reconnects THEN Stay Paused`() {
        // Arrange: 2 players offline (Bob & Charlie). Host online.
        val pausedData =
            baseData.copy(
                players =
                    mapOf(
                        playerId to player.copy(isConnected = false), // Bob
                        "p3" to Player("Charlie", "Green", "p3", isConnected = false), // Charlie
                        hostId to Player("Host", "Red", hostId, isConnected = true),
                    ),
                phaseAfterPause = GamePhase.InRound,
            )
        val msg = ClientMessage.RegisterPlayer("Bob", playerId)

        // Act: Bob reconnects (Charlie still missing)
        val result = sessionManager.registerPlayer(pausedData, GamePhase.Paused, msg)

        // Assert
        val valid = result as GameStateTransition.Valid

        // 1. Phase stays Paused
        assertEquals(GamePhase.Paused, valid.newPhase)

        // 2. phaseBeforePause persisted
        assertEquals(GamePhase.InRound, valid.newGameData.phaseAfterPause)

        // 3. NO GameResumed message
        val messages =
            valid.envelopes.map {
                when (it) {
                    is Envelope.Broadcast -> it.message
                    is Envelope.Unicast -> it.message
                }
            }
        assertFalse(messages.contains(ServerMessage.GameResumed(GamePhase.InRound)))
    }

    @Test
    fun `GIVEN Reconnection WHEN RoundData is Question THEN Syncs Sliced Data`() {
        // Arrange: Round Data has 5 pairs, current index 0. Sync should only send index 0.
        val roundData =
            RoundData.QuestionRoundData(
                roundPairs = listOf("p1" to "p2", "p2" to "p3"),
                currentPairIndex = 0,
            )
        val gameData =
            baseData.copy(
                players = mapOf(playerId to player.copy(isConnected = false)),
                roundData = roundData,
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
        assertEquals(GamePhase.InRound, valid.newGameData.phaseAfterPause)

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
        val pausedData =
            baseData.copy(
                players =
                    mapOf(
                        hostId to Player("Host", "Red", hostId, isConnected = false),
                        playerId to player, // Bob is currently online
                    ),
                phaseBeforePause = GamePhase.GameVoting,
                phaseAfterPause = GamePhase.GameVoting, // Was voting before pause
            )

        val event = SystemEvent.PlayerDisconnected(playerId) // Bob disconnects too

        // Act
        val result = sessionManager.handleSystemEvent(pausedData, GamePhase.Paused, event)
        val valid = result as GameStateTransition.Valid

        // Assert
        assertEquals(GamePhase.Paused, valid.newPhase)

        // CRITICAL: Ensure we didn't overwrite 'GameVoting' with 'Paused'
        assertEquals(GamePhase.GameVoting, valid.newGameData.phaseAfterPause)

        assertFalse(valid.newGameData.players[playerId]!!.isConnected)
    }

    @Test
    fun `GIVEN Unknown Player WHEN Disconnects THEN Ignore`() {
        val event = SystemEvent.PlayerDisconnected("unknown_id")
        val result = sessionManager.handleSystemEvent(baseData, GamePhase.InRound, event)

        assertTrue(result is GameStateTransition.Invalid)
    }

    // ==========================================
    // 4. KICK LOGIC
    // ==========================================

    @Test
    fun `GIVEN Active Game WHEN Imposter is Kicked THEN Civilians Win Immediately`() {
        // Arrange
        val imposterId = playerId
        val gameData =
            baseData.copy(
                imposterId = imposterId,
                roundNumber = 1,
                players =
                    baseData.players +
                        mapOf(
                            hostId to
                                Player(
                                    "Host",
                                    "Green",
                                    hostId,
                                    isConnected = true,
                                ),
                            ("123" to Player("Dummy", "Yellow", "123", isConnected = true)),
                        ),
            )

        // Mock Strategy (needed because kickPlayer calls it if not imposter/min players)
        // But here we hit the Imposter check first, so strategy shouldn't be called for logic.
        val mockStrategy = mockk<GameModeStrategy>()

        // Act
        val result =
            sessionManager.kickPlayer(gameData, GamePhase.InRound, imposterId, mockStrategy)

        // Assert
        assertTrue(result is GameStateTransition.Valid)
        val valid = result as GameStateTransition.Valid

        assertEquals(GamePhase.GameResults, valid.newPhase)

        val messages = valid.envelopes.map { (it as Envelope.Broadcast).message }
        assertTrue(messages.any { it is ServerMessage.VoteResult }) // Reveal
    }

    @Test
    fun `GIVEN Active Game WHEN Too Few Players Remain THEN End Game`() {
        // Arrange: Only 2 players. If 1 is kicked, we have 1 left. Min is 2.
        val data = baseData // Host + Player (Size 2)
        val mockStrategy = mockk<GameModeStrategy>()

        // Act
        val result = sessionManager.kickPlayer(data, GamePhase.InRound, playerId, mockStrategy)

        // Assert
        assertTrue(result is GameStateTransition.Valid)
        assertEquals(GamePhase.GameEnd, (result as GameStateTransition.Valid).newPhase)
    }

    @Test
    fun `GIVEN Paused Game WHEN Kicking Offline Player Makes Everyone Online THEN Resume Game`() {
        // Arrange: Game paused because P2 and P3 disconnected.
        val p3Id = "p3"
        val p3 = Player("Charlie", "Green", p3Id, isConnected = true) // Offline

        // Data has 3 players. P2 (online for this test setup), P3 (offline).
        // Wait, to test Resume, we need the result state to have ALL players connected.
        // So: Host(Online), P2(Offline).
        // If we KICK P2, only Host remains (Online). So "Everyone is connected".

        val pausedData =
            GameData(
                imposterId = hostId,
                players =
                    mapOf(
                        hostId to player.copy(id = hostId, isConnected = true),
                        playerId to player.copy(isConnected = false),
                        p3Id to p3,
                    ),
                phaseBeforePause = GamePhase.InRound,
                phaseAfterPause = GamePhase.InRound,
            )

        // Mock Strategy: Must return a valid transition for the kick logic
        val mockStrategy = mockk<GameModeStrategy>()
        every {
            mockStrategy.onPlayerRemoved(
                any(),
                any(),
                any(),
            )
        } returns
                GameStateTransition.Valid(
                newGameData = pausedData.copy(players = pausedData.players - playerId), // simplified return
                newPhase = GamePhase.InRound,
            )

        // Act: Kick the offline player
        val result = sessionManager.kickPlayer(pausedData, GamePhase.Paused, playerId, mockStrategy)

        // Assert
        assertTrue(result is GameStateTransition.Valid)
        val valid = result as GameStateTransition.Valid

        // Logic:
        // 1. Kick removes P2.
        // 2. Remaining: Host (Connected), P3 (Disconnected).
        // 3. isEveryoneConnected == true.
        // 4. Resume to phaseBeforePause (InRound).

        // Note: Our mock returned a blank GameData(), but the SessionManager logic wraps it.
        // The key is checking if it sends "GameResumed".

        val envelopes = valid.envelopes.filterIsInstance<Envelope.Broadcast>()
        assertTrue(envelopes.any { it.message is ServerMessage.GameResumed })
    }

    // ==========================================
    // 5. RECONNECTION STATE SYNC (The Fixes)
    // ==========================================

    @Test
    fun `GIVEN Imposter Reconnects WHEN Syncing THEN Receives ImposterId and NO Word`() {
        // Arrange
        val imposterId = playerId
        val data =
            baseData.copy(
                imposterId = imposterId,
                word = "SecretWord",
                players = mapOf(playerId to player.copy(isConnected = false)),
            )
        val msg = ClientMessage.RegisterPlayer("Bob", playerId)

        // Act
        val result = sessionManager.registerPlayer(data, GamePhase.InRound, msg)
        val valid = result as GameStateTransition.Valid

        // Extract Sync Message
        val syncEnvelope = valid.envelopes.find { it is Envelope.Unicast } as Envelope.Unicast
        val syncMsg = syncEnvelope.message as ServerMessage.ReconnectionFullStateSync

        // Assert
        assertEquals("Imposter should see their own ID", imposterId, syncMsg.data.imposterId)
        assertNull("Imposter should NOT see the word", syncMsg.data.word)
    }

    @Test
    fun `GIVEN Civilian Reconnects WHEN Syncing THEN Receives Word and NO ImposterId`() {
        // Arrange
        val civId = playerId
        val imposterId = hostId // Host is imposter
        val data =
            baseData.copy(
                imposterId = imposterId,
                word = "SecretWord",
                players = mapOf(playerId to player.copy(isConnected = false)),
            )
        val msg = ClientMessage.RegisterPlayer("Bob", civId)

        // Act
        val result = sessionManager.registerPlayer(data, GamePhase.InRound, msg)
        val valid = result as GameStateTransition.Valid

        val syncEnvelope = valid.envelopes.find { it is Envelope.Unicast } as Envelope.Unicast
        val syncMsg = syncEnvelope.message as ServerMessage.ReconnectionFullStateSync

        // Assert
        assertNull("Civilian should NOT see imposter ID", syncMsg.data.imposterId)
        assertEquals("Civilian SHOULD see the word", "SecretWord", syncMsg.data.word)
    }

    @Test
    fun `GIVEN Reconnect InRound WHEN Syncing THEN Receives FULL Round Data (Not Sliced)`() {
        // Arrange
        val roundData =
            RoundData.QuestionRoundData(
                roundPairs = listOf("p1" to "p2", "p2" to "p3", "p3" to "p1"),
                currentPairIndex = 1, // Middle of round
            )
        val data =
            baseData.copy(
                roundData = roundData,
                players = mapOf(playerId to player.copy(isConnected = false)),
            )
        val msg = ClientMessage.RegisterPlayer("Bob", playerId)

        // Act
        val result = sessionManager.registerPlayer(data, GamePhase.InRound, msg)

        // Assert
        val valid = result as GameStateTransition.Valid
        val syncMsg =
            (valid.envelopes.find { it is Envelope.Unicast } as Envelope.Unicast).message as ServerMessage.ReconnectionFullStateSync
        val syncRound = syncMsg.data.roundData as RoundData.QuestionRoundData

        assertEquals("Should send pairs until current round", 2, syncRound.roundPairs.size)
        assertEquals("Should preserve index", 1, syncRound.currentPairIndex)
    }
}
