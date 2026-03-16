package com.example.nsddemo.domain.logic

import android.util.Log
import com.example.nsddemo.domain.model.GameCategory
import com.example.nsddemo.domain.model.GameData
import com.example.nsddemo.domain.model.GamePhase
import com.example.nsddemo.domain.model.Player
import com.example.nsddemo.domain.model.RoundData
import com.example.nsddemo.domain.model.ServerMessage
import io.mockk.every
import io.mockk.mockkStatic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ClientStateReducerTest {
    // --- Helper Data ---
    private val localId = "local_p1"
    private val p1 = Player("Alice", "Red", localId, isConnected = true)
    private val p2 = Player("Bob", "Blue", "p2", isConnected = true)

    // A standard starting state
    private val baseData =
        GameData(
            localPlayerId = localId,
            hostId = localId,
            players = mapOf(localId to p1, "p2" to p2),
        )

    @Before
    fun setUp() {
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
    }

    // ==========================================
    // 1. LOBBY & SETUP
    // ==========================================

    @Test
    fun `GIVEN PlayerList Message WHEN Reduced THEN Replaces Player Map`() {
        val newPlayerList = listOf(p1, p2, Player("Charlie", "Green", "p3"))
        val msg = ServerMessage.PlayerList(newPlayerList)

        val result = ClientStateReducer.reduce(baseData, msg)

        assertEquals(3, result.players.size)
        assertTrue(result.players.containsKey("p3"))
    }

    @Test
    fun `GIVEN RegisterHost Message WHEN Reduced THEN Updates Host ID`() {
        val newHostId = "new_host_uuid"
        val msg = ServerMessage.RegisterHost(newHostId)

        val result = ClientStateReducer.reduce(baseData, msg)

        assertEquals(newHostId, result.hostId)
    }

    @Test
    fun `GIVEN CategorySelected Message WHEN Reduced THEN Updates Category`() {
        val msg = ServerMessage.CategorySelected(GameCategory.FOOD)
        val result = ClientStateReducer.reduce(baseData, msg)
        assertEquals(GameCategory.FOOD, result.category)
    }

    // ==========================================
    // 2. ROLE DISTRIBUTION
    // ==========================================

    @Test
    fun `GIVEN RoleAssigned (Innocent) WHEN Reduced THEN Sets Word and Null Imposter`() {
        val msg = ServerMessage.RoleAssigned(GameCategory.ANIMALS, "Lion")

        val result = ClientStateReducer.reduce(baseData, msg)

        assertEquals("Lion", result.word)
        assertEquals(GameCategory.ANIMALS, result.category)
        assertNull("Innocent shouldn't know imposter ID", result.imposterId)
    }

    @Test
    fun `GIVEN RoleAssigned (Imposter) WHEN Reduced THEN Sets Null Word and Local Imposter`() {
        // Empty word signifies Imposter in your logic
        val msg = ServerMessage.RoleAssigned(GameCategory.ANIMALS, "")

        val result = ClientStateReducer.reduce(baseData, msg)

        assertNull("Imposter sees no word", result.word)
        assertEquals("Imposter knows themself", localId, result.imposterId)
    }

    @Test
    fun `GIVEN PlayerReady Message WHEN Reduced THEN Updates Ready Set`() {
        val msg = ServerMessage.PlayerReady(listOf("p1", "p2"))

        val result = ClientStateReducer.reduce(baseData, msg)
        assertEquals(2, result.readyPlayerIds.size)
        assertTrue(result.readyPlayerIds.contains("p1"))
    }

    // ==========================================
    // 3. GAMEPLAY (ROUND LOGIC)
    // ==========================================

    @Test
    fun `GIVEN Question Message WHEN Current Round Idle THEN Creates New List`() {
        val idleData = baseData.copy(roundData = RoundData.Idle)
        val msg =
            ServerMessage.Question(
                "p1",
                "p2",
            ) // isFirst not strictly needed by your current logic branch

        val result = ClientStateReducer.reduce(idleData, msg)

        val round = result.roundData as RoundData.QuestionRoundData
        assertEquals(1, round.roundPairs.size)
        assertEquals("p1" to "p2", round.roundPairs[0])
        assertEquals(0, round.currentPairIndex)
    }

    @Test
    fun `GIVEN Question Message WHEN Current Round Exists THEN Appends and Increments`() {
        // Arrange: Existing pair at index 0
        val existingRound =
            RoundData.QuestionRoundData(
                roundPairs = listOf("p1" to "p2"),
                currentPairIndex = 0,
            )
        val activeData = baseData.copy(roundData = existingRound)

        // Act: Next question arrives
        val msg = ServerMessage.Question("p2", "p3")
        val result = ClientStateReducer.reduce(activeData, msg)

        // Assert
        val round = result.roundData as RoundData.QuestionRoundData
        assertEquals(2, round.roundPairs.size) // List grew
        assertEquals("p2" to "p3", round.roundPairs[1])
        assertEquals(1, round.currentPairIndex) // Index moved up
    }

    @Test
    fun `GIVEN RoundEnd Message WHEN Reduced THEN No Change (Persist Last Question)`() {
        val data =
            baseData.copy(
                roundData = RoundData.QuestionRoundData(listOf("a" to "b"), 0),
            )
        val msg = ServerMessage.RoundEnd
        val result = ClientStateReducer.reduce(data, msg)

        // Data should NOT reset to idle yet (prevents UI flicker)
        assertEquals(data, result)
    }

    @Test
    fun `GIVEN ReplayRound Message WHEN Reduced THEN Increments Round and Resets Data`() {
        val data =
            baseData.copy(
                roundNumber = 1,
                roundData = RoundData.QuestionRoundData(listOf("a" to "b"), 0),
            )
        val msg = ServerMessage.ReplayRound()
        val result = ClientStateReducer.reduce(data, msg)

        assertEquals(2, result.roundNumber)
        assertTrue(result.roundData is RoundData.Idle)
    }

    // ==========================================
    // 4. VOTING & RESULTS
    // ==========================================

    @Test
    fun `GIVEN PlayerVoted Message WHEN Reduced THEN Updates Votes Map`() {
        val msg = ServerMessage.PlayerVoted("p2", "p1") // p2 voted for p1
        val result = ClientStateReducer.reduce(baseData, msg)

        assertEquals("p1", result.votes["p2"])
    }

    @Test
    fun `GIVEN VoteResult Message WHEN Reduced THEN Updates All Scores`() {
        val votes = mapOf("p1" to "p2", "p2" to "p1")
        val scores = mapOf("p1" to 100, "p2" to 0)
        val imposter = "p1"

        val msg = ServerMessage.VoteResult(votes, imposter, scores)
        val result = ClientStateReducer.reduce(baseData, msg)

        assertEquals(votes, result.votes)
        assertEquals(scores, result.scores)
        assertEquals(imposter, result.imposterId)
    }

    @Test
    fun `GIVEN StartVote Message WHEN Reduced THEN Returns Same Data`() {
        // Your code currently does nothing on StartVote.
        // (Architectural Note: Usually this is where you clear old votes/voters)
        val msg = ServerMessage.StartVote
        val result = ClientStateReducer.reduce(baseData, msg)
        assertEquals(baseData, result)
    }

    // ==========================================
    // 5. SESSION MANAGEMENT
    // ==========================================

    @Test
    fun `GIVEN PlayerDisconnected Message WHEN Player Exists THEN Mark Offline`() {
        val msg = ServerMessage.PlayerDisconnected("p2")
        val result = ClientStateReducer.reduce(baseData, msg)

        assertFalse(result.players["p2"]!!.isConnected)
    }

    @Test
    fun `GIVEN PlayerDisconnected Message WHEN Player Missing THEN No Change`() {
        val msg = ServerMessage.PlayerDisconnected("ghost")
        val result = ClientStateReducer.reduce(baseData, msg)
        assertEquals(baseData, result)
    }

    @Test
    fun `GIVEN GameResumed Message WHEN Reduced THEN Clears PhaseAfterPause`() {
        // Arrange: Game is currently paused, waiting to go back to InRound
        val pausedData =
            baseData.copy(
                phaseAfterPause = GamePhase.InRound,
            )
        val msg = ServerMessage.GameResumed(GamePhase.InRound)

        // Act
        val result = ClientStateReducer.reduce(pausedData, msg)

        // Assert
        assertNull("phaseAfterPause should be cleared on resume", result.phaseAfterPause)
    }

    @Test
    fun `GIVEN VotesAfterLeaver Message WHEN Reduced THEN Replaces Votes Map`() {
        // Arrange: Old votes included a player who just left
        val oldVotes = mapOf("p1" to "p2", "p2" to "p3", "leaver" to "p2")
        val dataWithVotes = baseData.copy(votes = oldVotes)

        // Message contains cleaned votes
        val newVotes = mapOf("p1" to "p2", "p2" to "p3")
        val msg = ServerMessage.VotesAfterLeaver(newVotes)

        // Act
        val result = ClientStateReducer.reduce(dataWithVotes, msg)

        // Assert
        assertEquals(newVotes, result.votes)
        assertFalse(result.votes.containsKey("leaver"))
    }

    @Test
    fun `GIVEN ScoresAfterLeaver Message WHEN Reduced THEN Replaces Scores Map`() {
        // Arrange: Old scores included a player who just left
        val oldScores = mapOf("p1" to 100, "leaver" to 50)
        val dataWithScores = baseData.copy(scores = oldScores)

        // Message contains cleaned scores
        val newScores = mapOf("p1" to 100)
        val msg = ServerMessage.ScoresAfterLeaver(newScores)

        // Act
        val result = ClientStateReducer.reduce(dataWithScores, msg)

        // Assert
        assertEquals(newScores, result.scores)
        assertFalse(result.scores.containsKey("leaver"))
    }

    @Test
    fun `GIVEN PlayerReconnected Message WHEN Reduced THEN Updates and Adds Player`() {
        val updatedP2 = p2.copy(name = "Bob Back", isConnected = true)
        val msg = ServerMessage.PlayerReconnected(updatedP2)

        val result = ClientStateReducer.reduce(baseData, msg)

        assertEquals("Bob Back", result.players["p2"]!!.name)
        assertTrue(result.players["p2"]!!.isConnected)
    }

    @Test
    fun `GIVEN ReconnectionSync Message WHEN Reduced THEN Overwrites Data BUT Keeps LocalID`() {
        // Arrange:
        // Local Data: I am "local_p1"
        // Server Message Data: Says localPlayerId is "" (empty from session manager)
        val serverState =
            GameData(
                localPlayerId = "",
                hostId = "host",
                gameCode = "SYNCED",
            )
        val msg = ServerMessage.ReconnectionFullStateSync(serverState, GamePhase.InRound)

        // Act
        val result = ClientStateReducer.reduce(baseData, msg)

        // Assert
        assertEquals("SYNCED", result.gameCode) // Took server data
        assertEquals(localId, result.localPlayerId) // Kept local ID
    }

    // ==========================================
    // 6. GAME LIFECYCLE (RESET)
    // ==========================================

    @Test
    fun `GIVEN ReplayGame Message WHEN Reduced THEN Soft Reset (Keep Scores)`() {
        val dirtyData =
            baseData.copy(
                roundNumber = 5,
                word = "Word",
                category = GameCategory.FOOD,
                scores = mapOf(localId to 500),
            )
        val msg = ServerMessage.ReplayGame

        val result = ClientStateReducer.reduce(dirtyData, msg)

        // Verify Reset
        assertEquals(1, result.roundNumber)
        assertNull(result.category)
        assertNull(result.word)

        // Verify Persisted
        assertEquals(500, result.scores[localId])
        assertEquals(localId, result.localPlayerId)
        assertEquals(baseData.hostId, result.hostId)
        assertEquals(baseData.gameCode, result.gameCode)
    }

    @Test
    fun `GIVEN EndGame Message WHEN Reduced THEN Hard Reset`() {
        val msg = ServerMessage.EndGame
        val result = ClientStateReducer.reduce(baseData, msg)

        assertEquals(GameData(), result) // Totally empty
    }

    // ==========================================
    // 7. EDGE CASES & SAFETY
    // ==========================================

    @Test
    fun `GIVEN Player was Imposter WHEN RoleAssigned Innocent THEN ImposterId Cleared`() {
        // Arrange: Previous state where local player was the imposter
        val imposterData = baseData.copy(imposterId = localId, word = null)

        // Act: Assigned Innocent role (has a word)
        val msg = ServerMessage.RoleAssigned(GameCategory.FOOD, "Burger")
        val result = ClientStateReducer.reduce(imposterData, msg)

        // Assert
        assertEquals("Burger", result.word)
        assertNull("Imposter ID should be cleared", result.imposterId)
    }

    @Test
    fun `GIVEN No-Op Message WHEN Reduced THEN Returns Same Instance`() {
        // Messages that shouldn't trigger data changes
        val noOpMessages =
            listOf(
                ServerMessage.RoundEnd,
                ServerMessage.GameFull,
                ServerMessage.GameAlreadyStarted,
                ServerMessage.ContinueToGameChoice,
                ServerMessage.StartVote,
            )

        noOpMessages.forEach { msg ->
            val result = ClientStateReducer.reduce(baseData, msg)
            assertSame(
                "Message ${msg::class.simpleName} should return same instance",
                baseData,
                result,
            )
        }
    }

    @Test
    fun `GIVEN PlayerDisconnected WHEN Unknown Player THEN Returns Same Instance`() {
        val msg = ServerMessage.PlayerDisconnected("unknown_id")
        val result = ClientStateReducer.reduce(baseData, msg)

        assertSame(baseData, result)
    }
}
