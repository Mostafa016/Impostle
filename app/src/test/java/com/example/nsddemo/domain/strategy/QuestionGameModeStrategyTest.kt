package com.example.nsddemo.domain.strategy

import com.example.nsddemo.domain.model.ClientMessage
import com.example.nsddemo.domain.model.Envelope
import com.example.nsddemo.domain.model.GameCategory
import com.example.nsddemo.domain.model.GamePhase
import com.example.nsddemo.domain.model.GameStateTransition
import com.example.nsddemo.domain.model.NewGameData
import com.example.nsddemo.domain.model.Player
import com.example.nsddemo.domain.model.RoundData
import com.example.nsddemo.domain.model.ServerMessage
import com.example.nsddemo.domain.repository.WordRepository
import com.example.nsddemo.domain.util.GameFlowRegistry
import com.example.nsddemo.domain.util.GameScoreIncrements
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class QuestionGameModeStrategyTest {

    private lateinit var strategy: QuestionGameModeStrategy
    private lateinit var wordRepository: WordRepository

    // Helper Data
    private val hostId = "host_1"
    private val p2Id = "p2"
    private val p3Id = "p3"

    private val player1 = Player("Alice", "Red", hostId, isConnected = true)
    private val player2 = Player("Bob", "Blue", p2Id, isConnected = true)
    private val player3 = Player("Charlie", "Green", p3Id, isConnected = true)

    private val playersMap = mapOf(
        player1.id to player1,
        player2.id to player2,
        player3.id to player3
    )

    // A standard base state
    private val baseData = NewGameData(
        hostId = hostId,
        players = playersMap,
        category = GameCategory.ANIMALS
    )

    @Before
    fun setUp() {
        wordRepository = mockk()
        every { wordRepository.getWordsForCategory(any()) } returns listOf("Lion", "Tiger")

        // Mock the Registry to avoid dependency on its implementation status
        mockkObject(GameFlowRegistry)
        // Default behavior: Allow everything. Specific tests can override if needed.
        every { GameFlowRegistry.getValidPhasesFor(any()) } answers {
            // Allow the phase passed in the test arguments
            GamePhase::class.sealedSubclasses.mapNotNull { it.objectInstance }.toSet()
        }

        strategy = QuestionGameModeStrategy(wordRepository)
    }

    @After
    fun tearDown() {
        unmockkObject(GameFlowRegistry)
    }

    // ==========================================
    // 1. LOBBY & STARTUP
    // ==========================================

    @Test
    fun `GIVEN Lobby WHEN Host Selects Category THEN Success`() {
        val msg = ClientMessage.RequestSelectCategory(GameCategory.FOOD)
        val result = strategy.handleAction(baseData, GamePhase.Lobby, msg, hostId)

        assertTrue(result is GameStateTransition.Valid)
        val valid = result as GameStateTransition.Valid
        assertEquals(GameCategory.FOOD, valid.newGameData.category)
        assertTrue(valid.envelopes.first() is Envelope.Broadcast)
    }

    @Test
    fun `GIVEN Lobby WHEN Non-Host Selects Category THEN Invalid`() {
        val msg = ClientMessage.RequestSelectCategory(GameCategory.FOOD)
        val result = strategy.handleAction(baseData, GamePhase.Lobby, msg, p2Id)
        assertTrue(result is GameStateTransition.Invalid)
    }

    @Test
    fun `GIVEN Lobby WHEN Host Starts Game (Happy Path) THEN Transition to RoleDistribution`() {
        val result =
            strategy.handleAction(baseData, GamePhase.Lobby, ClientMessage.RequestStartGame, hostId)

        assertTrue(result is GameStateTransition.Valid)
        val valid = result as GameStateTransition.Valid
        assertEquals(GamePhase.RoleDistribution, valid.newPhase)

        // Verify Roles Assigned
        assertNotNull(valid.newGameData.word)
        assertNotNull(valid.newGameData.imposterId)

        // Verify Question Round Data Initialized
        val roundData = valid.newGameData.roundData
        assertTrue(roundData is RoundData.QuestionRoundData)
        assertTrue((roundData as RoundData.QuestionRoundData).roundPairs.isNotEmpty())
    }

    @Test
    fun `GIVEN Lobby WHEN Start Game with No Category THEN Invalid`() {
        val dataNoCat = baseData.copy(category = null)
        val result = strategy.handleAction(
            dataNoCat,
            GamePhase.Lobby,
            ClientMessage.RequestStartGame,
            hostId
        )
        assertTrue((result as GameStateTransition.Invalid).reason.contains("No category"))
    }

    @Test
    fun `GIVEN Lobby WHEN Start Game with Insufficient Players THEN Invalid`() {
        val fewPlayers = baseData.copy(players = mapOf(hostId to player1)) // Only 1 player
        val result = strategy.handleAction(
            fewPlayers,
            GamePhase.Lobby,
            ClientMessage.RequestStartGame,
            hostId
        )
        assertTrue((result as GameStateTransition.Invalid).reason.contains("Not enough"))
    }

    @Test
    fun `GIVEN Wrong Phase WHEN Register Player THEN Invalid`() {
        // Mock Registry to restrict phases strictly for this test
        every { GameFlowRegistry.getValidPhasesFor(any()) } returns setOf(GamePhase.Lobby)

        val msg = ClientMessage.RegisterPlayer("Dave", "p4")
        val result =
            strategy.handleAction(baseData, GamePhase.InRound, msg, "p4") // InRound != Lobby

        assertTrue(result is GameStateTransition.Invalid)
    }

    // ==========================================
    // 2. ROLE CONFIRMATION (Template Hook)
    // ==========================================

    @Test
    fun `GIVEN RoleDistribution WHEN First Player Confirms THEN Update Ready Count`() {
        val roleData = baseData.copy(word = "Lion", imposterId = p2Id)
        val result = strategy.handleAction(
            roleData,
            GamePhase.RoleDistribution,
            ClientMessage.ConfirmRoleReceived,
            hostId
        )

        assertTrue(result is GameStateTransition.Valid)
        val valid = result as GameStateTransition.Valid
        assertTrue(valid.newGameData.readyPlayerIds.contains(hostId))
        assertNull(valid.newPhase) // No transition yet
    }

    @Test
    fun `GIVEN RoleDistribution WHEN Last Player Confirms THEN Start Round`() {
        val readyData = baseData.copy(
            word = "Lion", imposterId = p2Id,
            readyPlayerIds = setOf(hostId, p2Id)
        )
        // RoundData must be set from StartGame
        val pairs = listOf(hostId to p2Id, p2Id to p3Id, p3Id to hostId)
        val dataWithRound =
            readyData.copy(roundData = RoundData.QuestionRoundData(roundPairs = pairs))

        val result = strategy.handleAction(
            dataWithRound,
            GamePhase.RoleDistribution,
            ClientMessage.ConfirmRoleReceived,
            p3Id
        )

        val valid = result as GameStateTransition.Valid
        assertEquals(GamePhase.InRound, valid.newPhase)
        assertTrue(valid.newGameData.readyPlayerIds.isEmpty()) // Should be cleared

        // Verify Question Broadcast
        val broadcast = valid.envelopes.first() as Envelope.Broadcast
        val msg = broadcast.message as ServerMessage.Question
        assertEquals(hostId, msg.askerId) // First pair
    }

    // ==========================================
    // 3. IN ROUND (Question Logic)
    // ==========================================

    @Test
    fun `GIVEN InRound WHEN Asker Ends Turn THEN Increment Index`() {
        val pairs = listOf(hostId to p2Id, p2Id to p3Id)
        val roundData = RoundData.QuestionRoundData(roundPairs = pairs, currentPairIndex = 0)
        val data = baseData.copy(roundData = roundData)

        val result = strategy.handleAction(data, GamePhase.InRound, ClientMessage.EndTurn, hostId)

        val valid = result as GameStateTransition.Valid
        val newRound = valid.newGameData.roundData as RoundData.QuestionRoundData

        assertEquals(1, newRound.currentPairIndex)
        assertEquals(p2Id, newRound.currentAskerId)
    }

    @Test
    fun `GIVEN InRound WHEN Wrong Player Ends Turn THEN Invalid`() {
        val pairs = listOf(hostId to p2Id)
        val roundData = RoundData.QuestionRoundData(roundPairs = pairs, currentPairIndex = 0)
        val data = baseData.copy(roundData = roundData)

        // p2 tries to end turn, but p1 is asker
        val result = strategy.handleAction(data, GamePhase.InRound, ClientMessage.EndTurn, p2Id)
        assertTrue(result is GameStateTransition.Invalid)
    }

    @Test
    fun `GIVEN InRound WHEN Last Question Ends THEN Transition to ReplayChoice`() {
        val pairs = listOf(hostId to p2Id)
        val roundData =
            RoundData.QuestionRoundData(roundPairs = pairs, currentPairIndex = 0) // Last one
        val data = baseData.copy(roundData = roundData)

        val result = strategy.handleAction(data, GamePhase.InRound, ClientMessage.EndTurn, hostId)

        val valid = result as GameStateTransition.Valid
        assertEquals(GamePhase.RoundReplayChoice, valid.newPhase)
        assertEquals(
            ServerMessage.RoundEnd,
            (valid.envelopes.first() as Envelope.Broadcast).message
        )
    }

    // ==========================================
    // 4. ROUND REPLAY CHOICE
    // ==========================================

    @Test
    fun `GIVEN RoundReplayChoice WHEN Host Replays Round THEN Reset and Start`() {
        val data = baseData.copy(
            roundNumber = 1,
            word = "Lion", imposterId = p2Id,
            // Dirty state to ensure cleanup
            readyPlayerIds = setOf(hostId),
            votes = mapOf(hostId to p2Id)
        )

        val result = strategy.handleAction(
            data,
            GamePhase.RoundReplayChoice,
            ClientMessage.RequestReplayRound,
            hostId
        )

        val valid = result as GameStateTransition.Valid

        // 1. Data Reset
        assertEquals("Round should increment", 2, valid.newGameData.roundNumber)

        // 2. Phase
        // Based on logic, onRoundStart returns InRound
        assertEquals(GamePhase.InRound, valid.newPhase)

        // 3. New Pairs generated
        val roundData = valid.newGameData.roundData as RoundData.QuestionRoundData
        assertTrue(roundData.roundPairs.isNotEmpty())
    }

    @Test
    fun `GIVEN RoundReplayChoice WHEN Host Starts Vote THEN Transition to Voting`() {
        val result = strategy.handleAction(
            baseData,
            GamePhase.RoundReplayChoice,
            ClientMessage.RequestStartVote,
            hostId
        )

        val valid = result as GameStateTransition.Valid
        assertEquals(GamePhase.GameVoting, valid.newPhase)
        assertEquals(
            ServerMessage.StartVote,
            (valid.envelopes.first() as Envelope.Broadcast).message
        )
    }

    // ==========================================
    // 5. VOTING & RESULTS
    // ==========================================

    @Test
    fun `GIVEN Voting WHEN Player Votes Self THEN Invalid`() {
        val data = baseData.copy(imposterId = p3Id)
        val result = strategy.handleAction(
            data,
            GamePhase.GameVoting,
            ClientMessage.SubmitVote(hostId),
            hostId
        )
        assertTrue((result as GameStateTransition.Invalid).reason.contains("themselves"))
    }

    @Test
    fun `GIVEN Voting WHEN Player Votes Twice THEN Invalid`() {
        val data = baseData.copy(
            imposterId = p3Id,
            votes = mapOf(hostId to p2Id) // Host already voted
        )
        val result = strategy.handleAction(
            data,
            GamePhase.GameVoting,
            ClientMessage.SubmitVote(p3Id),
            hostId
        )
        assertTrue((result as GameStateTransition.Invalid).reason.contains("once"))
    }

    @Test
    fun `GIVEN Voting WHEN Vote Submitted (Not Last) THEN Update Data`() {
        val data = baseData.copy(imposterId = p3Id)
        val result = strategy.handleAction(
            data,
            GamePhase.GameVoting,
            ClientMessage.SubmitVote(p2Id),
            hostId
        )

        val valid = result as GameStateTransition.Valid
        assertEquals(p2Id, valid.newGameData.votes[hostId])
        assertNull(valid.newPhase) // Stay in Voting

        val msg = (valid.envelopes.first() as Envelope.Broadcast).message
        assertTrue(msg is ServerMessage.PlayerVoted)
    }

    @Test
    fun `GIVEN Voting WHEN Last Vote Submitted THEN Calculate Scores & Transition`() {
        val data = baseData.copy(
            imposterId = p3Id, // Imposter is P3
            votes = mapOf(hostId to p3Id, p2Id to p3Id), // P1 & P2 voted Correctly
            scores = mapOf(hostId to 0, p2Id to 0, p3Id to 0)
        )

        // P3 votes for P1 (Incorrect)
        val result = strategy.handleAction(
            data,
            GamePhase.GameVoting,
            ClientMessage.SubmitVote(hostId),
            p3Id
        )

        val valid = result as GameStateTransition.Valid
        assertEquals(GamePhase.GameResults, valid.newPhase)

        // Scores check
        val msg =
            valid.envelopes.find { (it as Envelope.Broadcast).message is ServerMessage.VoteResult }
        val resultMsg = (msg as Envelope.Broadcast).message as ServerMessage.VoteResult
        val scores = resultMsg.playerScores

        // Host & P2 should get points (Correct vote)
        assertEquals(GameScoreIncrements.CORRECT_PLAYER_GUESS, scores[hostId])
        assertEquals(GameScoreIncrements.CORRECT_PLAYER_GUESS, scores[p2Id])
        // P3 should get 0 (Incorrect vote)
        assertEquals(GameScoreIncrements.INCORRECT_PLAYER_GUESS, scores[p3Id])
    }

    // ==========================================
    // 6. END GAME & REPLAY GAME
    // ==========================================

    @Test
    fun `GIVEN Results WHEN Host Continues THEN Transition to ReplayChoice`() {
        val result = strategy.handleAction(
            baseData,
            GamePhase.GameResults,
            ClientMessage.RequestContinueToGameChoice,
            hostId
        )
        val valid = result as GameStateTransition.Valid
        assertEquals(GamePhase.GameReplayChoice, valid.newPhase)
    }

    @Test
    fun `GIVEN GameReplayChoice WHEN Host Replays Game THEN Soft Reset to Lobby`() {
        val dirtyData = baseData.copy(
            roundNumber = 5,
            word = "UsedWord",
            votes = mapOf(hostId to p2Id),
            scores = mapOf(hostId to 1000)
        )

        val result = strategy.handleAction(
            dirtyData,
            GamePhase.GameReplayChoice,
            ClientMessage.RequestReplayGame,
            hostId
        )
        val valid = result as GameStateTransition.Valid

        assertEquals(GamePhase.Lobby, valid.newPhase)

        // Verify Soft Reset
        assertEquals("Scores should persist", 1000, valid.newGameData.scores[hostId])
        assertNull("Category reset", valid.newGameData.category)
        assertNull("Word reset", valid.newGameData.word)
        assertTrue("Ready players cleared", valid.newGameData.readyPlayerIds.isEmpty())
        assertTrue("Votes cleared", valid.newGameData.votes.isEmpty())
        assertEquals("Round reset", 1, valid.newGameData.roundNumber)
    }

    @Test
    fun `GIVEN GameReplayChoice WHEN Host Ends Game THEN Hard Reset to Idle`() {
        val result = strategy.handleAction(
            baseData,
            GamePhase.GameReplayChoice,
            ClientMessage.RequestEndGame,
            hostId
        )
        val valid = result as GameStateTransition.Valid

        assertEquals(GamePhase.Idle, valid.newPhase)

        // Verify Hard Reset (Empty Data)
        assertTrue(valid.newGameData.players.isEmpty())
        assertTrue(valid.newGameData.scores.isEmpty())
    }
}