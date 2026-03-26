package com.mostafa.impostle.domain.integration

import com.mostafa.impostle.domain.model.GamePhase
import com.mostafa.impostle.domain.model.RoundData
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * E2E Disconnect / Reconnect CUJ
 *
 * Scenario:
 *  1. Alice (host), Bob, Charlie reach [GamePhase.InRound].
 *  2. Bob's connection is dropped (Wi-Fi cut) via [InMemoryNetworkRouter.dropConnection].
 *  3. All remaining players see [GamePhase.Paused].
 *  4. Bob reconnects using the **same playerId** (triggers [SessionManager.performReconnection]).
 *  5. All players resume the phase that was active before the drop.
 *  6. Bob receives a [ServerMessage.ReconnectionFullStateSync] and his local state is in sync.
 */
class DisconnectReconnectIntegrationTest : BaseIntegrationTest() {
    override val gameCode = "RECONNECT"

    @Test
    fun `GIVEN Bob disconnects mid-round WHEN Bob reconnects THEN game resumes for all players`() =
        runTest {
            // ── 1. Reach InRound ──────────────────────────────────────────────────
            advanceToInRound()

            val phaseBeforeDrop = alice.gamePhase.value // InRound
            assertEquals(GamePhase.InRound, phaseBeforeDrop)

            // ── 2. Drop Bob's connection ──────────────────────────────────────────
            router.dropConnection(gameCode, bob.playerId)
            advanceUntilIdle()

            // All connected players should see Paused
            assertEquals(GamePhase.Paused, alice.gamePhase.value)
            assertEquals(GamePhase.Paused, charlie.gamePhase.value)

            // ── 3. Bob reconnects with the same playerId ──────────────────────────
            // Create a new HeadlessPlayer with the *same* playerId — this is the
            // reconnection scenario. SessionManager.registerPlayer detects the existing
            // (disconnected) player entry and calls performReconnection().
            val bobReconnected =
                HeadlessPlayer(
                    playerId = bob.playerId, // same ID = reconnection path
                    name = "Bob",
                    gameCode = gameCode,
                    router = router,
                    isHost = false,
                )
            bobReconnected.startIn(this)
            advanceUntilIdle()

            bobReconnected.joinGame() // sends RegisterPlayer(same id) → triggers reconnection
            advanceUntilIdle()

            // ── 4. Assert all players resumed ─────────────────────────────────────
            // SessionManager.performReconnection auto-resumes when isEveryoneConnected.
            assertEquals(GamePhase.InRound, alice.gamePhase.value)
            assertEquals(GamePhase.InRound, charlie.gamePhase.value)

            // Bob's new instance receives a ReconnectionFullStateSync and enters the right phase
            assertEquals(GamePhase.InRound, bobReconnected.gamePhase.value)

            // Bob's state is synced: he can see all 3 players
            assertEquals(3, bobReconnected.gameData.value.players.size)

            // The round data should still be valid (current question pair is intact)
            val roundData = alice.gameData.value.roundData
            assertTrue(
                "Round data must be QuestionRoundData after resume",
                roundData is RoundData.QuestionRoundData,
            )
            assertNotNull((roundData as RoundData.QuestionRoundData).currentAskerId)

            // Explicitly stop all infinite loops so runTest finishes immediately
            alice.stop()
            bob.stop()
            charlie.stop()
            bobReconnected.stop()
        }

    @Test
    fun `GIVEN Bob disconnects in Lobby WHEN Bob disconnects THEN Bob is removed and no pause occurs`() =
        runTest {
            alice.startIn(this)
            bob.startIn(this)
            charlie.startIn(this)
            advanceUntilIdle()

            alice.joinGame()
            bob.joinGame()
            charlie.joinGame()
            advanceUntilIdle()
            assertEquals(3, alice.gameData.value.players.size)

            // Lobby disconnect → player is simply removed, no pause
            router.dropConnection(gameCode, bob.playerId)
            advanceUntilIdle()

            // Phase remains Lobby (no pause for Lobby disconnects)
            assertEquals(GamePhase.Lobby, alice.gamePhase.value)
            assertEquals(GamePhase.Lobby, charlie.gamePhase.value)

            // Bob is removed from the player list
            assertEquals(2, alice.gameData.value.players.size)
            assertEquals(2, charlie.gameData.value.players.size)

            // Explicitly stop all infinite loops
            alice.stop()
            bob.stop()
            charlie.stop()
        }
}
