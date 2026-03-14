package com.example.nsddemo.domain.e2e

import com.example.nsddemo.domain.model.GamePhase
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * E2E Happy Path CUJ
 *
 * Scenario: Alice (host) creates a game. Bob and Charlie join. Alice selects a
 * category and starts the game. All three confirm their roles. The question round
 * runs to completion (N pairs for N players). Alice calls start-vote; all three
 * vote for the same player. Results are shown. Alice continues to game choice,
 * then replays the game (back to Lobby). Alice ends the game — all clients receive
 * the [ClientEvent.LobbyClosed] event.
 *
 * Because this entire test runs inside [runTest] with [StandardTestDispatcher],
 * all coroutines and [delay] calls use virtual time → executes in milliseconds.
 */
class HappyPathE2ETest : BaseE2ETest() {

    override val gameCode = "HAPPY"

    @Test
    fun `GIVEN 3 players WHEN full game round played THEN phases progress correctly and lobby re-entered`() =
        runTest {
            advanceToInRound()

            // ── 5. Play through all N question pairs ─────────────────────────────
            // With 3 players there are exactly 3 (asker, asked) pairs.
            // The asker for each pair calls endTurn(). The first N-1 endTurns advance the
            // index; the Nth endTurn triggers RoundReplayChoice (isLastQuestion).
            //
            // We don't know the order (pairs are shuffled), so we loop until the server
            val players = listOf(alice, bob, charlie)
            exhaustQuestionPairs(players)

            // ── 6. All players vote ───────────────────────────────────────────────
            driveVoting(players)

            // ── 7. Imposter Guesses Word Randomly ───────────────────────────────────────────────
            driveImposterGuess(players)
            assertNotNull(alice.gameData.value.imposterId)
            assertEquals(3, alice.gameData.value.votes.size)

            // ── 8. Host continues to game choice ─────────────────────────────────
            alice.continueToGameChoice()
            advanceUntilIdle()

            assertEquals(GamePhase.GameReplayChoice, alice.gamePhase.value)
            assertEquals(GamePhase.GameReplayChoice, bob.gamePhase.value)
            assertEquals(GamePhase.GameReplayChoice, charlie.gamePhase.value)
            // ── 9. Host replays game → back to Lobby ─────────────────────────────
            alice.replayGame()
            advanceUntilIdle()

            assertEquals(GamePhase.Lobby, alice.gamePhase.value)
            assertEquals(GamePhase.Lobby, bob.gamePhase.value)
            assertEquals(GamePhase.Lobby, charlie.gamePhase.value)

            players.forEach {
                it.stop()
            }
        }
}
