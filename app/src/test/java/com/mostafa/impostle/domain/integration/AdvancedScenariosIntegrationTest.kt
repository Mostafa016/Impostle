package com.mostafa.impostle.domain.integration

import com.mostafa.impostle.domain.model.GameCategory
import com.mostafa.impostle.domain.model.GamePhase
import com.mostafa.impostle.domain.model.RoundData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * E2E tests for advanced multi-step scenarios:
 *
 *  - **Multi-Round Game**: Host replays the round twice before calling a vote.
 *    Verifies the round number increments and new question pairs are generated
 *    each time.
 *
 *  - **Replay Game (second match)**: After a full game completes and all players
 *    vote, the host calls `replayGame`. This resets all game state (word, imposter,
 *    votes, round data) while preserving cumulative scores and the player roster.
 *    A second full game is then played end-to-end.
 *
 *  - **End Game → GameEnd Phase**: After results, host calls `endGame` (sends
 *    `ServerMessage.EndGame`). Verifies that all clients reach [GamePhase.GameEnd].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AdvancedScenariosIntegrationTest : BaseIntegrationTest() {
    override val gameCode = "ADVANCED"

    // ═══════════════════════════════════════════════════════════════════════════
    // Scenario 1: Multi-Round Game
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `GIVEN round ends WHEN host replays round THEN round number increments and new pairs generated`() =
        runTest {
            // ── Round 1: exhaust all pairs ────────────────────────────────────────
            advanceToInRound()
            val players = listOf(alice, bob, charlie)
            exhaustQuestionPairs(players)
            assertEquals(GamePhase.RoundReplayChoice, alice.gamePhase.value)
            assertEquals(1, alice.gameData.value.roundNumber) // Still round 1 at choice screen

            // ── Host chooses to replay the round ──────────────────────────────────
            alice.replayRound()
            advanceUntilIdle()

            // Server transitions back to InRound with incremented round number
            assertEquals(GamePhase.InRound, alice.gamePhase.value)
            assertEquals(GamePhase.InRound, bob.gamePhase.value)
            assertEquals(GamePhase.InRound, charlie.gamePhase.value)
            assertEquals(2, alice.gameData.value.roundNumber)

            // New round data should have fresh pairs
            val roundTwoData = alice.gameData.value.roundData as RoundData.QuestionRoundData
            assertEquals(0, roundTwoData.currentPairIndex) // Reset to start

            // ── Round 2: exhaust all pairs ────────────────────────────────────────
            exhaustQuestionPairs(players)
            assertEquals(GamePhase.RoundReplayChoice, alice.gamePhase.value)
            assertEquals(2, alice.gameData.value.roundNumber)

            // ── Proceed to vote and results ───────────────────────────────────────
            driveVoting(players)
            assertEquals(GamePhase.ImposterGuess, alice.gamePhase.value)
            assertEquals(3, alice.gameData.value.votes.size)

            alice.stop()
            bob.stop()
            charlie.stop()
        }

    @Test
    fun `GIVEN round played twice WHEN round number checked THEN round numbers reflect full replay history`() =
        runTest {
            advanceToInRound()

            val players = listOf(alice, bob, charlie)

            // Play 3 rounds total, verifying round number increments correctly each time
            repeat(3) { iteration ->
                val expectedRoundNumber = iteration + 1
                assertEquals(expectedRoundNumber, alice.gameData.value.roundNumber)

                exhaustQuestionPairs(players)
                assertEquals(GamePhase.RoundReplayChoice, alice.gamePhase.value)

                if (iteration < 2) {
                    alice.replayRound()
                    advanceUntilIdle()
                    assertEquals(GamePhase.InRound, alice.gamePhase.value)
                }
            }

            // Should be at RoundReplayChoice after 3rd round, with roundNumber == 3
            assertEquals(GamePhase.RoundReplayChoice, alice.gamePhase.value)
            assertEquals(3, alice.gameData.value.roundNumber)

            alice.stop()
            bob.stop()
            charlie.stop()
        }

    // ═══════════════════════════════════════════════════════════════════════════
    // Scenario 2: Replay Game (full second match)
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `GIVEN game completed WHEN host replays game THEN state resets to Lobby and second game plays through`() =
        runTest {
            advanceToInRound()

            val players = listOf(alice, bob, charlie)

            // ── Game 1: play through to results ──────────────────────────────────
            exhaustQuestionPairs(players)
            driveVoting(players)
            driveImposterGuess(players)

            val game1Scores = alice.gameData.value.scores
            assertTrue(
                "Scores should be populated after voting",
                game1Scores.isNotEmpty(),
            )

            // ── Continue → game replay choice ─────────────────────────────────────
            alice.continueToGameChoice()
            advanceUntilIdle()
            assertEquals(GamePhase.GameReplayChoice, alice.gamePhase.value)
            players.forEach { assertEquals(GamePhase.GameReplayChoice, it.gamePhase.value) }

            // ── Replay game → back to Lobby ───────────────────────────────────────
            alice.replayGame()
            advanceUntilIdle()

            assertEquals(GamePhase.Lobby, alice.gamePhase.value)
            assertEquals(GamePhase.Lobby, bob.gamePhase.value)
            assertEquals(GamePhase.Lobby, charlie.gamePhase.value)

            // Verify state was correctly reset for a fresh game
            // — Word, imposter, votes and round data are cleared
            assertEquals(null, alice.gameData.value.word)
            assertEquals(null, alice.gameData.value.imposterId)
            assertTrue(
                "Votes should be empty after replay",
                alice.gameData.value.votes
                    .isEmpty(),
            )
            assertEquals(1, alice.gameData.value.roundNumber) // Round resets to 1
            // — But cumulative scores persist
            val scoresAfterReplay = alice.gameData.value.scores
            assertEquals(
                "Scores should be preserved after replayGame",
                game1Scores.keys,
                scoresAfterReplay.keys,
            )
            // — All 3 players still in the roster
            assertEquals(3, alice.gameData.value.players.size)

            // ── Game 2: play a complete second game ───────────────────────────────
            // Need to re-select category and restart (Lobby is clean for game start)
            alice.selectCategory(GameCategory.ANIMALS)
            advanceUntilIdle()

            alice.startGame()
            advanceUntilIdle()

            assertEquals(GamePhase.RoleDistribution, alice.gamePhase.value)
            players.forEach { assertEquals(GamePhase.RoleDistribution, it.gamePhase.value) }

            // Confirm roles and play game 2 all the way through
            alice.confirmRole()
            bob.confirmRole()
            charlie.confirmRole()
            advanceUntilIdle()
            assertEquals(GamePhase.InRound, alice.gamePhase.value)

            exhaustQuestionPairs(players)
            driveVoting(players)
            driveImposterGuess(players)
            assertEquals(GamePhase.GameResults, alice.gamePhase.value)

            // Game 2 scores should be higher than (or equal to) game 1 (cumulative)
            val game2Scores = alice.gameData.value.scores
            val totalGame2Points = game2Scores.values.sum()
            val totalGame1Points = game1Scores.values.sum()
            assertTrue(
                "Cumulative scores after game 2 should be >= game 1 scores",
                totalGame2Points >= totalGame1Points,
            )

            alice.stop()
            bob.stop()
            charlie.stop()
        }

    // ═══════════════════════════════════════════════════════════════════════════
    // Scenario 3: End Game → GameEnd Phase
    // ═══════════════════════════════════════════════════════════════════════════
    @Test
    fun `GIVEN game at results WHEN host ends game THEN all clients reach GameEnd phase`() =
        runTest {
            alice.startIn(this)
            bob.startIn(this)
            charlie.startIn(this)
            advanceUntilIdle()
            advanceToInRound()

            val players = listOf(alice, bob, charlie)
            exhaustQuestionPairs(players)
            driveVoting(players)
            driveImposterGuess(players)

            // Continue to game replay choice screen
            alice.continueToGameChoice()
            advanceUntilIdle()
            assertEquals(GamePhase.GameReplayChoice, alice.gamePhase.value)

            // ── Act: Host calls endGame (valid from GameReplayChoice) ─────────────
            // Server broadcasts ServerMessage.EndGame → clients get GamePhase.GameEnd
            alice.endGame()
            advanceUntilIdle()

            // ── Assert: all clients reach GameEnd ─────────────────────────────────
            assertEquals(GamePhase.GameEnd, alice.gamePhase.value)
            assertEquals(GamePhase.GameEnd, bob.gamePhase.value)
            assertEquals(GamePhase.GameEnd, charlie.gamePhase.value)

            alice.stop()
            bob.stop()
            charlie.stop()
        }

    @Test
    fun `GIVEN game at GameReplayChoice WHEN host chooses endGame over replay THEN game does not re-enter Lobby`() =
        runTest {
            alice.startIn(this)
            bob.startIn(this)
            charlie.startIn(this)
            advanceUntilIdle()
            advanceToInRound()

            val players = listOf(alice, bob, charlie)
            exhaustQuestionPairs(players)
            driveVoting(players)
            driveImposterGuess(players)
            alice.continueToGameChoice()
            advanceUntilIdle()

            // Verify we are NOT in Lobby before the action
            assertNotEquals(GamePhase.Lobby, alice.gamePhase.value)

            alice.endGame()
            advanceUntilIdle()

            // Result must be GameEnd, NOT Lobby
            assertEquals(GamePhase.GameEnd, alice.gamePhase.value)
            assertNotEquals(GamePhase.Lobby, alice.gamePhase.value)

            alice.stop()
            bob.stop()
            charlie.stop()
        }

    // ═══════════════════════════════════════════════════════════════════════════
    // Scenario 4: End Game From Paused (via pause → endGame)
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `GIVEN game paused WHEN host ends game from Paused THEN all clients reach GameEnd`() =
        runTest {
            advanceToInRound()

            // Drop Bob → game pauses
            router.dropConnection(gameCode, bob.playerId)
            advanceUntilIdle()
            assertEquals(GamePhase.Paused, alice.gamePhase.value)
            assertEquals(GamePhase.Paused, charlie.gamePhase.value)

            // endGame is valid from Paused (per GameFlowRegistry, RequestEndGame allows Paused)
            alice.endGame()
            advanceUntilIdle()

            assertEquals(GamePhase.GameEnd, alice.gamePhase.value)
            assertEquals(GamePhase.GameEnd, charlie.gamePhase.value)

            // Explicitly stop all infinite loops
            alice.stop()
            bob.stop()
            charlie.stop()
        }
}
