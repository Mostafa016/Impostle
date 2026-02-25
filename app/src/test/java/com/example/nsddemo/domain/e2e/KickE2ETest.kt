// --- START OF FILE KickE2ETest.kt ---

package com.example.nsddemo.domain.e2e

import app.cash.turbine.test
import com.example.nsddemo.domain.model.ClientEvent
import com.example.nsddemo.domain.model.GamePhase
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * E2E Kick CUJ
 *
 * Scenario A (civilian kicked from pause screen):
 *  1. Alice (host), Bob, Charlie reach [GamePhase.InRound].
 *  2. Bob (civilian) disconnects -> Game goes to [GamePhase.Paused].
 *  3. Alice kicks Bob.
 *  4. Bob receives [ClientEvent.KickedFromGame].
 *  5. Alice and Charlie see a new round started automatically (game auto-resumes
 *     because all *remaining* players are connected — no pause when host kicks).
 *  6. Players list shrinks to 2.
 *
 * Scenario B (imposter kicked from pause screen):
 *  1. Alice (host), Bob, Charlie reach [GamePhase.InRound].
 *  2. Imposter disconnects -> Game goes to [GamePhase.Paused].
 *  3. Alice kicks the imposter.
 *  4. All remaining players jump to [GamePhase.GameResults] (civilians win).
 */
class KickE2ETest : BaseE2ETest() {

    override val gameCode = "KICK"

    @Test
    fun `GIVEN civilian disconnected WHEN host kicks them THEN player removed and round restarts for remaining`() =
        runTest {
            advanceToInRound()

            val imposterId = getImposterId(listOf(alice, bob, charlie))
            val civilianToKick = listOf(bob, charlie).first { it.playerId != imposterId }
            val remainingNonHost = listOf(bob, charlie).first { it != civilianToKick }

            // ── 1. Force the civilian to disconnect FIRST ─────────────────────────
            router.dropConnection(gameCode, civilianToKick.playerId)
            advanceUntilIdle()

            // Verify the state machine correctly protected the game by pausing
            assertEquals(GamePhase.Paused, alice.gamePhase.value)
            assertEquals(GamePhase.Paused, remainingNonHost.gamePhase.value)

            // ── 2. Host kicks the disconnected civilian ───────────────────────────
            alice.kickPlayer(civilianToKick.playerId)
            advanceUntilIdle()

            // ── 3. Assert remaining players resumed correctly ─────────────────────
            // With 2 remaining players (>= MIN_PLAYERS=2), the game auto-resumes.
            // It goes from Paused -> InRound cleanly.
            assertEquals(GamePhase.InRound, alice.gamePhase.value)
            assertEquals(GamePhase.InRound, remainingNonHost.gamePhase.value)

            assertEquals(2, alice.gameData.value.players.size)
            assertFalse(alice.gameData.value.players.containsKey(civilianToKick.playerId))

            alice.stop(); bob.stop(); charlie.stop()
        }

    @Test
    fun `GIVEN imposter disconnected WHEN host kicks them THEN civilians win and phase is GameResults`() =
        runTest {
            advanceToInRound()

            val imposterId = getImposterId(listOf(alice, bob, charlie))
            val imposterPlayer = listOf(bob, charlie).first { it.playerId == imposterId }
            val nonImposter = listOf(bob, charlie).first { it.playerId != imposterId }

            // ── 1. Force the imposter to disconnect FIRST ─────────────────────────
            router.dropConnection(gameCode, imposterId)
            advanceUntilIdle()

            assertEquals(GamePhase.Paused, alice.gamePhase.value)
            assertEquals(GamePhase.Paused, nonImposter.gamePhase.value)
            // ── 2. Host kicks the disconnected imposter ───────────────────────────
            alice.kickPlayer(imposterId)
            advanceUntilIdle()

            // ── 3. Assert game immediately jumps to results ───────────────────────
            // Goes from Paused -> GameResults cleanly
            assertEquals(GamePhase.GameResults, alice.gamePhase.value)
            assertEquals(GamePhase.GameResults, nonImposter.gamePhase.value)

            assertEquals(imposterId, alice.gameData.value.imposterId)

            alice.stop(); bob.stop(); charlie.stop()
        }

    @Test
    fun `GIVEN host kicks player in Lobby WHEN kicked THEN player removed and lobby phase preserved`() =
        runTest {
            alice.startIn(this)
            bob.startIn(this)
            charlie.startIn(this)
            advanceUntilIdle()

            alice.joinGame(); bob.joinGame(); charlie.joinGame()
            advanceUntilIdle()
            assertEquals(3, alice.gameData.value.players.size)

            // Lobby kicks are allowed natively because Lobby isn't an "Active" phase
            bob.clientEvents.test {
                alice.kickPlayer(bob.playerId)
                advanceUntilIdle()
                assertEquals(ClientEvent.KickedFromGame, awaitItem())
            }

            assertEquals(GamePhase.Lobby, alice.gamePhase.value)
            assertEquals(2, alice.gameData.value.players.size)

            alice.stop(); bob.stop(); charlie.stop()
        }
}