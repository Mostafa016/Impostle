package com.example.nsddemo.domain.e2e

import android.util.Log
import com.example.nsddemo.domain.e2e.fakes.InMemoryNetworkRouter
import com.example.nsddemo.domain.model.GameCategory
import com.example.nsddemo.domain.model.GamePhase
import com.example.nsddemo.domain.model.RoundData
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before

/**
 * Common base class for E2E tests.
 *
 * Provides:
 * - Proper [Log] mocking to prevent JVM crashes.
 * - Standard [StandardTestDispatcher] setup and teardown.
 * - Shared [InMemoryNetworkRouter] and default players (alice = host, bob, charlie).
 * - Common utility functions for fast-forwarding game state and driving standard bot actions.
 */
@ExperimentalCoroutinesApi
abstract class BaseE2ETest {
    protected lateinit var router: InMemoryNetworkRouter
    protected lateinit var alice: HeadlessPlayer
    protected lateinit var bob: HeadlessPlayer
    protected lateinit var charlie: HeadlessPlayer

    // Derived classes should specify their own game code
    abstract val gameCode: String

    @Before
    open fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } answers {
            println("E2E-ERROR: ${secondArg<String>()} :: ${thirdArg<Throwable>()}")
            0
        }

        router = InMemoryNetworkRouter()
        alice = HeadlessPlayer("alice", "Alice", gameCode, router, isHost = true)
        bob = HeadlessPlayer("bob", "Bob", gameCode, router, isHost = false)
        charlie = HeadlessPlayer("charlie", "Charlie", gameCode, router, isHost = false)
    }

    @After
    open fun tearDown() {
        unmockkAll()
        Dispatchers.resetMain()
    }

    // ─── Shared Test Utilities ───────────────────────────────────────────────────

    /**
     * Starts the standard 3 players, joins the lobby, selects a category,
     * starts the game, and confirms roles. Fast-forwards directly to [GamePhase.InRound].
     */
    protected suspend fun TestScope.advanceToInRound(category: GameCategory = GameCategory.ANIMALS) {
        alice.startIn(this)
        bob.startIn(this)
        charlie.startIn(this)
        advanceUntilIdle()

        alice.joinGame()
        bob.joinGame()
        charlie.joinGame()
        advanceUntilIdle()

        alice.selectCategory(category)
        advanceUntilIdle()

        alice.startGame()
        advanceUntilIdle()

        // All three confirm roles
        alice.confirmRole()
        bob.confirmRole()
        charlie.confirmRole()
        advanceUntilIdle()

        assertEquals(GamePhase.InRound, alice.gamePhase.value)
        assertEquals(GamePhase.InRound, bob.gamePhase.value)
        assertEquals(GamePhase.InRound, charlie.gamePhase.value)
    }

    /**
     * Exhausts all N question pairs for N players by having the current asker
     * call endTurn() for each pair until [GamePhase.RoundReplayChoice] is reached.
     */
    protected suspend fun TestScope.exhaustQuestionPairs(players: List<HeadlessPlayer>) {
        val n = players.size
        repeat(n) {
            val rd = alice.gameData.value.roundData as RoundData.QuestionRoundData
            val asker = players.first { it.playerId == rd.currentAskerId }
            asker.endTurn()
            advanceUntilIdle()
        }
        assertEquals(GamePhase.RoundReplayChoice, alice.gamePhase.value)
        assertEquals(GamePhase.RoundReplayChoice, bob.gamePhase.value)
        assertEquals(GamePhase.RoundReplayChoice, charlie.gamePhase.value)
    }

    /**
     * Drives all N players to vote. By default, everyone votes for the first player
     * in the list who is NOT the voter.
     * Returns when [GamePhase.GameResults] is reached.
     */
    protected suspend fun TestScope.driveVoting(players: List<HeadlessPlayer>) {
        alice.startVote()
        advanceUntilIdle()
        assertEquals(GamePhase.GameVoting, alice.gamePhase.value)
        assertEquals(GamePhase.GameVoting, bob.gamePhase.value)
        assertEquals(GamePhase.GameVoting, charlie.gamePhase.value)

        players.forEach { voter ->
            val target = players.first { it.playerId != voter.playerId }
            voter.submitVote(target.playerId)
            advanceUntilIdle()
        }
        assertEquals(GamePhase.ImposterGuess, alice.gamePhase.value)
        assertEquals(GamePhase.ImposterGuess, bob.gamePhase.value)
        assertEquals(GamePhase.ImposterGuess, charlie.gamePhase.value)
    }

    /**
     * Drives the imposter to guess the secret word. By default, the imposter guesses a random word
     * from the words provided to them that is guaranteed to be **incorrect**.
     *
     * Returns when [GamePhase.GameResults] is reached.
     */
    protected suspend fun TestScope.driveImposterGuess(players: List<HeadlessPlayer>) {
        val imposterId = getImposterId(players)
        val imposterPlayer = players.find { it.playerId == imposterId }!!
        val correctWord =
            players
                .first { it.playerId != imposterId }
                .gameData.value.word!!

        val wordGuessChoices =
            imposterPlayer.gameData.value.wordOptions
                .filterNot { it != correctWord }
                .random()
        imposterPlayer.submitImposterGuess(wordGuessChoices)
        advanceUntilIdle()

        assertEquals(GamePhase.GameResults, alice.gamePhase.value)
        assertEquals(GamePhase.GameResults, bob.gamePhase.value)
        assertEquals(GamePhase.GameResults, charlie.gamePhase.value)
    }

    /**
     * Helper to find the imposter among a list of players.
     */
    protected fun getImposterId(players: List<HeadlessPlayer>): String = players.first { it.gameData.value.word == null }.playerId
}
