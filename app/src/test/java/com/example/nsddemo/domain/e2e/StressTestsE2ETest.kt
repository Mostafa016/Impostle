package com.example.nsddemo.domain.e2e

import android.util.Log
import com.example.nsddemo.domain.e2e.fakes.InMemoryNetworkRouter
import com.example.nsddemo.domain.model.GameCategory
import com.example.nsddemo.domain.model.GamePhase
import com.example.nsddemo.domain.model.RoundData
import com.example.nsddemo.domain.util.PlayerCountLimits
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

/**
 * Phase 3: Stress & Edge-Case Testing
 *
 * Verifies that the GameServer's sequential action processing, Flow merging,
 * and SessionManager rules hold up under high concurrency and extreme edge cases.
 */
@ExperimentalCoroutinesApi
class StressTestsE2ETest {

    private lateinit var router: InMemoryNetworkRouter
    private lateinit var alice: HeadlessPlayer // host

    companion object {
        private const val GAME_CODE = "STRESS_TEST"
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } answers {
            println("E2E-STRESS-ERROR: ${secondArg<String>()} :: ${thirdArg<Throwable>()}")
            0
        }

        router = InMemoryNetworkRouter()
        alice = HeadlessPlayer("alice", "Host Alice", GAME_CODE, router, isHost = true)
    }

    @After
    fun tearDown() {
        unmockkAll()
        Dispatchers.resetMain()
    }

    // ─── Scenario 1: Thundering Herd Join ────────────────────────────────────────

    @Test
    fun `1 GIVEN host started WHEN 31 clients join instantly THEN 32 player limit enforced and no crashes`() =
        runTest {
            alice.startIn(this)
            alice.joinGame()
            advanceUntilIdle()

            // 32 players max. We have 1 (Host). Let's spawn 35 to guarantee we hit the limit.
            val extraClients = (1..35).map { i ->
                HeadlessPlayer("bot_$i", "Bot $i", GAME_CODE, router, isHost = false)
            }

            // Start all listening coroutines immediately
            val startJobs = extraClients.map { bot ->
                launch { bot.startIn(this@runTest) }
            }
            startJobs.joinAll()

            // Thundering herd: all send RegisterPlayer in the exact same virtual tick
            val joinJobs = extraClients.map { bot ->
                launch { bot.joinGame() }
            }
            joinJobs.joinAll()
            advanceUntilIdle() // Process all incoming messages

            val finalData = alice.gameData.value

            // Assert exact max limits were enforced, no ArrayOutOfBounds or ConcurrentModification
            assertEquals(
                "Player count must be strictly capped at max (${PlayerCountLimits.MAX_PLAYERS})",
                PlayerCountLimits.MAX_PLAYERS,
                finalData.players.size
            )

            // Cleanup
            alice.stop()
            extraClients.forEach { it.stop() }
        }

    // ─── Scenario 2: Concurrent Vote Submission ──────────────────────────────────

    @Test
    fun `2 GIVEN 4 players in voting WHEN everyone submits exact same tick THEN exactly 1 GameResults phase achieved`() =
        runTest {
            val players = mutableListOf(alice)
            // Need exactly 1 host + 3 clients = 4 total for this test
            for (i in 1..3) {
                players.add(HeadlessPlayer("bot_$i", "Bot $i", GAME_CODE, router, false))
            }

            players.forEach { it.startIn(this) }
            advanceUntilIdle()

            players.forEach { it.joinGame() }
            advanceUntilIdle()

            // Quick fast-forward to GameVoting
            alice.selectCategory(GameCategory.ANIMALS)
            alice.startGame()
            advanceUntilIdle()

            players.forEach { it.confirmRole() }
            advanceUntilIdle()

            // Exhaust question pairs
            repeat(players.size) {
                val rd = alice.gameData.value.roundData as RoundData.QuestionRoundData
                val asker = players.first { it.playerId == rd.currentAskerId }
                asker.endTurn()
                advanceUntilIdle()
            }

            alice.startVote()
            advanceUntilIdle()
            assertEquals(GamePhase.GameVoting, alice.gamePhase.value)

            val voteJobs = players.map { voter ->
                launch {
                    // Everyone votes for Bot 1 (guaranteed to not be Alice, simplifying)
                    voter.submitVote(if (voter.playerId == players[1].playerId) players[2].playerId else players[1].playerId)
                }
            }
            voteJobs.joinAll()
            advanceUntilIdle()

            // If a race condition occurred, the `merge` flow could double-process the final
            // vote counting, triggering state errors. Assert clean land:
            assertEquals(GamePhase.ImposterGuess, alice.gamePhase.value)
            assertEquals(4, alice.gameData.value.votes.size)

            players.forEach { it.stop() }
        }

    // ─── Scenario 3: Rapid Disconnect/Reconnect ─────────────────────────────────

    @Test
    fun `3 GIVEN mid-round WHEN player drops and re-joins 10 times instantly THEN state does not corrupt`() =
        runTest {
            val bob = HeadlessPlayer("bob", "Bob", GAME_CODE, router, false)
            val charlie = HeadlessPlayer("charlie", "Charlie", GAME_CODE, router, false)
            val players = listOf(alice, bob, charlie)

            players.forEach { it.startIn(this) }
            advanceUntilIdle()

            players.forEach { it.joinGame() }
            advanceUntilIdle()

            alice.selectCategory(GameCategory.ANIMALS)
            alice.startGame()
            advanceUntilIdle()

            players.forEach { it.confirmRole() }
            advanceUntilIdle()

            assertEquals(GamePhase.InRound, alice.gamePhase.value)

            var latestBob = bob

            // The bounce
            repeat(10) {
                // Drop connection
                router.dropConnection(GAME_CODE, latestBob.playerId)
                advanceUntilIdle()

                // Assert properly paused
                assertEquals(GamePhase.Paused, alice.gamePhase.value)
                assertFalse(alice.gameData.value.players[latestBob.playerId]!!.isConnected)

                // Instantiate new client (same ID) to simulate reconnect
                latestBob = HeadlessPlayer("bob", "Bob", GAME_CODE, router, false)
                latestBob.joinGame()
                advanceUntilIdle()

                // Assert properly resumed
                assertEquals(GamePhase.InRound, alice.gamePhase.value)
                assertEquals(true, alice.gameData.value.players[latestBob.playerId]?.isConnected)
            }

            // Assert roster size remains exactly 3 — no "ghost players" were appended
            assertEquals(3, alice.gameData.value.players.size)

            alice.stop(); charlie.stop(); bob.stop()
        }

    // ─── Scenario 4: Sequential Replay Bombing ──────────────────────────────────

    @Test
    fun `4 GIVEN completed game WHEN host spams replayGame THEN sequence handles back-to-back resets firmly`() =
        runTest {
            val bob = HeadlessPlayer("bob", "Bob", GAME_CODE, router, false)
            val charlie = HeadlessPlayer("charlie", "Charlie", GAME_CODE, router, false)
            val players = listOf(alice, bob, charlie)

            players.forEach { it.startIn(this) }
            advanceUntilIdle()

            players.forEach { it.joinGame() }
            advanceUntilIdle()

            alice.selectCategory(GameCategory.ANIMALS)
            alice.startGame()
            advanceUntilIdle()
            players.forEach { it.confirmRole() }
            advanceUntilIdle()

            // Fast forward to GameReplayChoice
            repeat(3) {
                val rd = alice.gameData.value.roundData as RoundData.QuestionRoundData
                players.first { it.playerId == rd.currentAskerId }.endTurn()
                advanceUntilIdle()
            }
            alice.startVote()
            advanceUntilIdle()
            players.forEach { voter ->
                val target = players.first { it.playerId != voter.playerId }
                voter.submitVote(target.playerId)
                advanceUntilIdle()
            }

            assertEquals(GamePhase.ImposterGuess, alice.gamePhase.value)

            val imposterId = players.first { it.gameData.value.word == null }.playerId
            val imposterPlayer = players.find { it.playerId == imposterId }!!

            val wordGuessChoices = imposterPlayer.gameData.value.wordOptions.random()
            imposterPlayer.submitImposterGuess(wordGuessChoices)
            advanceUntilIdle()

            assertEquals(GamePhase.GameResults, alice.gamePhase.value)


            alice.continueToGameChoice()
            advanceUntilIdle()

            assertEquals(GamePhase.GameReplayChoice, alice.gamePhase.value)

            // Spam Replay (RequestReplayGame).
            // A human can only do this once. If a stray double-tap occurs, the
            // SessionManager or flow shouldn't crash if it receives it while in Lobby.
            repeat(5) {
                alice.replayGame()
            }
            advanceUntilIdle()

            // The first one transitioned us to Lobby. The subsequent 4 arrived during
            // Lobby, which GameFlowRegistry maps `RequestReplayGame` to Invalid.
            // Invalid transitions are ignored safely (Log.e). Result should be a stable Lobby.
            assertEquals(GamePhase.Lobby, alice.gamePhase.value)

            // Prove the game is fully playable after the spam
            alice.selectCategory(GameCategory.FOOD)
            advanceUntilIdle()

            alice.startGame()
            advanceUntilIdle()
            assertEquals(GamePhase.RoleDistribution, alice.gamePhase.value)

            alice.stop(); bob.stop(); charlie.stop()
        }
}
