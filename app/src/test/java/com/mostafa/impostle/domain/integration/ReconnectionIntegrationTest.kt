package com.mostafa.impostle.domain.integration

import app.cash.turbine.test
import com.mostafa.impostle.domain.model.ClientEvent
import com.mostafa.impostle.domain.model.GameCategory
import com.mostafa.impostle.domain.model.GamePhase
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Phase 3.5: Reconnection Integration Tests
 *
 * Dedicated tests verifying the full GameClient ↔ router ↔ GameServer ↔ SessionManager
 * reconnection logic loop. Focuses on state synchronization (`ReconnectionFullStateSync`),
 * role privacy (nulling imposter ID/word), partial reconnects, and `GameResumed` emissions.
 */
class ReconnectionIntegrationTest : BaseIntegrationTest() {
    override val gameCode = "RECONNECT_SYNC"

    private suspend fun kotlinx.coroutines.test.TestScope.reconnectPlayer(playerTemplate: HeadlessPlayer): HeadlessPlayer {
        val reconnected =
            HeadlessPlayer(
                playerId = playerTemplate.playerId,
                name = playerTemplate.name,
                gameCode = gameCode,
                router = router,
                isHost = false,
            )
        reconnected.startIn(this)
        advanceUntilIdle()
        reconnected.joinGame()
        advanceUntilIdle()
        return reconnected
    }

    // ─── Core Test Scenarios ────────────────────────────────────────────────────────

    @Test
    fun `1 GIVEN InRound disconnect WHEN player reconnects THEN all clients receive GameResumed and update phase`() =
        runTest {
            advanceToInRound()

            // Disconnect Bob -> game pauses
            router.dropConnection(gameCode, bob.playerId)
            advanceUntilIdle()
            assertEquals(GamePhase.Paused, alice.gamePhase.value)
            assertEquals(GamePhase.Paused, charlie.gamePhase.value)

            // Reconnect Bob
            val bobReconnected = reconnectPlayer(bob)

            // Assert resume
            assertEquals(GamePhase.InRound, alice.gamePhase.value)
            assertEquals(GamePhase.InRound, charlie.gamePhase.value)
            assertEquals(GamePhase.InRound, bobReconnected.gamePhase.value)

            alice.stop()
            charlie.stop()
            bobReconnected.stop()
            bob.stop()
        }

    @Test
    fun `2 GIVEN player disconnected WHEN player reconnects THEN reconnected client receives ReconnectionFullStateSync`() =
        runTest {
            advanceToInRound()
            router.dropConnection(gameCode, charlie.playerId)
            advanceUntilIdle()

            val charlieReconnected = reconnectPlayer(charlie)

            // Verify full state synced: players list, hostId, category are all correct
            val syncedData = charlieReconnected.gameData.value
            assertEquals(3, syncedData.players.size)
            assertEquals(alice.playerId, syncedData.hostId)
            assertEquals(GameCategory.ANIMALS, syncedData.category)

            alice.stop()
            bob.stop()
            charlieReconnected.stop()
            charlie.stop()
        }

    @Test
    fun `3 GIVEN civilian reconnects THEN synced data hides imposterId and provides word`() =
        runTest {
            advanceToInRound(ensureClientImposter = true)

            val imposterId = getImposterId(listOf(bob, charlie))
            val civilianToDrop = listOf(bob, charlie).first { it.playerId != imposterId }

            router.dropConnection(gameCode, civilianToDrop.playerId)
            advanceUntilIdle()

            val civilianReconnected = reconnectPlayer(civilianToDrop)
            val syncedData = civilianReconnected.gameData.value

            // Privacy checks for civilian:
            // 1. Must NOT know the imposter
            assertNull("Civilian should not receive imposterId in sync data", syncedData.imposterId)
            // 2. Must receive the actual word
            assertNotNull("Civilian must receive the word in sync data", syncedData.word)

            alice.stop()
            bob.stop()
            charlie.stop()
            civilianReconnected.stop()
        }

    @Test
    fun `4 GIVEN imposter reconnects THEN synced data provides null word`() =
        runTest {
            advanceToInRound(ensureClientImposter = true)

            val imposterId = getImposterId(listOf(bob, charlie))
            val imposterPlayer = listOf(bob, charlie).first { it.playerId == imposterId }

            router.dropConnection(gameCode, imposterPlayer.playerId)
            advanceUntilIdle()

            val imposterReconnected = reconnectPlayer(imposterPlayer)
            val syncedData = imposterReconnected.gameData.value

            // Privacy checks for imposter:
            // 1. Knows they are the imposter
            assertEquals(imposterId, syncedData.imposterId)
            // 2. Word must be null (imposter doesn't know the word)
            assertNull("Imposter should receive null word in sync data", syncedData.word)

            alice.stop()
            bob.stop()
            charlie.stop()
            imposterReconnected.stop()
        }

    @Test
    fun `5 GIVEN two players drop WHEN only one reconnects THEN game remains paused`() =
        runTest {
            advanceToInRound()

            // Drop BOTH Bob and Charlie
            router.dropConnection(gameCode, bob.playerId)
            router.dropConnection(gameCode, charlie.playerId)
            advanceUntilIdle()

            assertEquals(GamePhase.Paused, alice.gamePhase.value)

            // Reconnect ONLY Bob
            val bobReconnected = reconnectPlayer(bob)

            // Game should STILL be paused because Charlie is missing
            assertEquals(GamePhase.Paused, alice.gamePhase.value)
            assertEquals(GamePhase.Paused, bobReconnected.gamePhase.value)

            alice.stop()
            bobReconnected.stop()
            charlie.stop()
            bob.stop()
        }

    @Test
    fun `6 GIVEN RoleDistribution disconnect WHEN player reconnects THEN game resumes in RoleDistribution`() =
        runTest {
            // Start components
            alice.startIn(this)
            bob.startIn(this)
            charlie.startIn(this)
            advanceUntilIdle()

            // Reach Lobby
            alice.joinGame()
            bob.joinGame()
            charlie.joinGame()
            advanceUntilIdle()

            // Select category and start game -> lands in RoleDistribution
            alice.selectCategory(GameCategory.ANIMALS)
            advanceUntilIdle()
            alice.startGame()
            advanceUntilIdle()

            assertEquals(GamePhase.RoleDistribution, alice.gamePhase.value)

            // Drop charlie BEFORE anyone confirms role
            router.dropConnection(gameCode, charlie.playerId)
            advanceUntilIdle()

            assertEquals(GamePhase.Paused, alice.gamePhase.value)
            assertEquals(GamePhase.Paused, bob.gamePhase.value)

            // Reconnect charlie
            val charlieReconnected = reconnectPlayer(charlie)

            // Assert everything resumed to RoleDistribution
            assertEquals(GamePhase.RoleDistribution, alice.gamePhase.value)
            assertEquals(GamePhase.RoleDistribution, bob.gamePhase.value)
            assertEquals(GamePhase.RoleDistribution, charlieReconnected.gamePhase.value)

            alice.stop()
            bob.stop()
            charlie.stop()
            charlieReconnected.stop()
        }

    @Test
    fun `7 GIVEN game resumes WHEN all connect THEN GameResumed event is emitted to clients`() =
        runTest {
            advanceToInRound()

            router.dropConnection(gameCode, bob.playerId)
            advanceUntilIdle()

            // Set up turbine to listen on charlie's client events
            charlie.clientEvents.test {
                // Ignore any events that fired before dropping connection (like PlayerRejoined from start)
                cancelAndIgnoreRemainingEvents()
            }

            // Fresh turbine for the resume event
            charlie.clientEvents.test {
                val bobReconnected = reconnectPlayer(bob)

                // Charlie sees Bob rejoin AND the game resume
                assertEquals(ClientEvent.PlayerRejoined(bob.playerId), awaitItem())
                assertEquals(ClientEvent.GameResumed, awaitItem())

                alice.stop()
                charlie.stop()
                bob.stop()
                bobReconnected.stop()
            }
        }
}
