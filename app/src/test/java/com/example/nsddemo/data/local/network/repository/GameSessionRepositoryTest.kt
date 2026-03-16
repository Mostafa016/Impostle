package com.example.nsddemo.data.local.network.repository

import com.example.nsddemo.data.repository.GameSessionRepositoryImpl
import com.example.nsddemo.domain.model.GameData
import com.example.nsddemo.domain.model.GamePhase
import com.example.nsddemo.domain.model.Player
import com.example.nsddemo.domain.model.RoundData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class GameSessionRepositoryTest {
    // SUT (System Under Test)
    private lateinit var repository: GameSessionRepositoryImpl

    @Before
    fun setUp() {
        repository = GameSessionRepositoryImpl()
    }

    // --- State Initialization Tests ---
    @Test
    fun `GIVEN new repository WHEN checked THEN starts in Idle state with empty data`() {
        assertEquals("Initial state should be Idle", GamePhase.Idle, repository.gameState.value)
        assertEquals("Initial data should be empty", GameData(), repository.gameData.value)

        // Verify RoundData is Idle by default
        assertTrue(repository.gameData.value.roundData is RoundData.Idle)
    }

    // --- Data Update Tests ---
    @Test
    fun `WHEN updateGameData is called THEN data is modified correctly`() {
        val player = Player("Alice", "Red", "p1")

        // 1. Update Host ID
        repository.updateGameData { it.copy(hostId = "host-1") }
        assertEquals("host-1", repository.gameData.value.hostId)

        // 2. Add Player (verifying it preserves previous Host ID)
        repository.updateGameData { it.copy(players = mapOf(player.id to player)) }

        assertEquals(1, repository.gameData.value.players.size)
        assertEquals(
            "Alice",
            repository.gameData.value.players["p1"]
                ?.name,
        )
        assertEquals("host-1", repository.gameData.value.hostId) // Previous data persists
    }

    @Test
    fun `GIVEN multiple threads WHEN updateGameData called concurrently THEN all updates are applied (No Race Conditions)`() =
        runTest {
            // Arrange
            val numberOfUpdates = 100

            // Initialize with QuestionRoundData so we have an index to increment
            repository.updateGameData {
                it.copy(
                    roundData =
                        RoundData.QuestionRoundData(
                            roundPairs = emptyList(),
                            currentPairIndex = 0,
                        ),
                )
            }

            // Act
            // Switch to Dispatchers.Default to ensure we are using a REAL thread pool.
            withContext(Dispatchers.Default) {
                val jobs =
                    List(numberOfUpdates) {
                        launch {
                            repository.updateGameData { oldData ->
                                // This lambda runs inside the spin-lock/mutex.
                                // We cast safely here because we know the state is QuestionRoundData
                                val currentRound = oldData.roundData as RoundData.QuestionRoundData

                                oldData.copy(
                                    roundData =
                                        currentRound.copy(
                                            currentPairIndex = currentRound.currentPairIndex + 1,
                                        ),
                                )
                            }
                        }
                    }
                jobs.joinAll()
            }

            // Assert
            val finalRoundData = repository.gameData.value.roundData as RoundData.QuestionRoundData

            // If race conditions existed, this number would be < 100 (lost updates)
            assertEquals(
                "Expected $numberOfUpdates incremental updates",
                numberOfUpdates,
                finalRoundData.currentPairIndex,
            )
        }

    // --- State Transition & Validation Tests ---
    @Test
    fun `GIVEN Idle state WHEN updating to Lobby (Valid) THEN state changes successfully`() {
        // Idle -> Lobby is valid
        repository.updateGamePhase(GamePhase.Lobby)
        assertEquals(GamePhase.Lobby, repository.gameState.value)
    }

    @Test
    fun `GIVEN Idle state WHEN updating to InRound (Invalid) THEN throws IllegalStateException`() {
        // Idle -> InRound is NOT allowed in GamePhase.validNextStates

        assertThrows(IllegalStateException::class.java) {
            repository.updateGamePhase(GamePhase.InRound)
        }

        // Verify the state remains unchanged
        assertEquals(
            "State should stay Idle after failed transition",
            GamePhase.Idle,
            repository.gameState.value,
        )
    }

    @Test
    fun `GIVEN Lobby state WHEN updating to Lobby (Self-Transition) THEN throws IllegalStateException`() {
        // Transition to Lobby first
        repository.updateGamePhase(GamePhase.Lobby)

        // Update to Lobby again
        assertThrows(IllegalStateException::class.java) {
            repository.updateGamePhase(GamePhase.Lobby)
        }
    }

    // --- Reset Tests ---
    @Test
    fun `GIVEN active game state WHEN reset is called THEN state becomes Idle and data is cleared`() {
        // Arrange: Dirty the state
        repository.updateGamePhase(GamePhase.Lobby)
        repository.updateGameData { oldGameData ->
            oldGameData.copy(
                localPlayerId = "123",
                gameCode = "ABCD",
                // Set round data to something other than Idle
                roundData = RoundData.QuestionRoundData(emptyList(), 5),
            )
        }

        // Act
        repository.reset()

        // Assert
        assertEquals("State should be reset to Idle", GamePhase.Idle, repository.gameState.value)
        assertEquals("Data should be reset to default", GameData(), repository.gameData.value)
        assertEquals("Local Player ID should be empty", "", repository.gameData.value.localPlayerId)
        assertTrue(
            "RoundData should be reset to Idle",
            repository.gameData.value.roundData is RoundData.Idle,
        )
    }
}
