package com.example.nsddemo.data.local.network.repository

import com.example.nsddemo.data.repository.GameSessionRepositoryImpl
import com.example.nsddemo.domain.model.Idle
import com.example.nsddemo.domain.model.Lobby
import com.example.nsddemo.domain.model.NewGameData
import com.example.nsddemo.domain.model.Player
import com.example.nsddemo.domain.model.RoundQuestions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
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
        assertEquals("Initial state should be Idle", Idle, repository.gameState.value)
        assertEquals("Initial data should be empty", NewGameData(), repository.gameData.value)
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
        assertEquals("Alice", repository.gameData.value.players["p1"]?.name)
        assertEquals("host-1", repository.gameData.value.hostId) // Previous data persists
    }

    @Test
    fun `GIVEN multiple threads WHEN updateGameData called concurrently THEN all updates are applied (No Race Conditions)`() =
        runTest {
            // Arrange
            val numberOfUpdates = 100

            // We use 'currentTurnIndex' as a counter to verify atomic accumulation
            assertEquals(0, repository.gameData.value.currentPairIndex)

            // Act
            // Switch to Dispatchers.Default to ensure we are using a REAL thread pool.
            // runTest's standard dispatcher is single-threaded, which wouldn't trigger race conditions.
            withContext(Dispatchers.Default) {
                val jobs = List(numberOfUpdates) {
                    launch {
                        repository.updateGameData { oldData ->
                            // This lambda runs inside the spin-lock.
                            // Even if multiple threads enter here, only one wins,
                            // and others re-run with the updated data.
                            oldData.copy(currentPairIndex = oldData.currentPairIndex + 1)
                        }
                    }
                }
                jobs.joinAll()
            }

            // Assert
            // If race conditions existed, this number would be < 100 (lost updates)
            assertEquals(
                "Expected $numberOfUpdates incremental updates",
                numberOfUpdates,
                repository.gameData.value.currentPairIndex
            )
        }

    // --- State Transition & Validation Tests ---
    @Test
    fun `GIVEN Idle state WHEN updating to Lobby (Valid) THEN state changes successfully`() {
        // Idle -> Lobby is valid
        repository.updateGameState(Lobby)
        assertEquals(Lobby, repository.gameState.value)
    }

    @Test
    fun `GIVEN Idle state WHEN updating to RoundQuestions (Invalid) THEN throws IllegalStateException`() {
        // Idle -> RoundQuestions is NOT allowed in GameState.validNextStates

        val exception = assertThrows(IllegalStateException::class.java) {
            repository.updateGameState(RoundQuestions)
        }

        // Verify the state remains unchanged
        assertEquals(
            "State should stay Idle after failed transition",
            Idle,
            repository.gameState.value
        )
    }

    @Test
    fun `GIVEN Lobby state WHEN updating to Lobby (Self-Transition) THEN throws IllegalStateException`() {
        // Transition to Lobby first
        repository.updateGameState(Lobby)

        // Update to Lobby again
        assertThrows(IllegalStateException::class.java) {
            repository.updateGameState(Lobby)
        }
    }

    // --- Reset Tests ---
    @Test
    fun `GIVEN active game state WHEN reset is called THEN state becomes Idle and data is cleared`() {
        // Arrange: Dirty the state
        repository.updateGameState(Lobby)
        repository.updateGameData { oldGameData -> oldGameData.copy(localPlayerId = "123") }
        repository.updateGameData { it.copy(gameCode = "ABCD") }

        // Act
        repository.reset()

        // Assert
        assertEquals("State should be reset to Idle", Idle, repository.gameState.value)
        assertEquals("Data should be reset to default", NewGameData(), repository.gameData.value)
        assertEquals("Local Player ID should be empty", "", repository.gameData.value.localPlayerId)
    }
}