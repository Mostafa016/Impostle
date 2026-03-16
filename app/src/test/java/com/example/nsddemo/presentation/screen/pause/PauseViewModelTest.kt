package com.example.nsddemo.presentation.screen.pause

import app.cash.turbine.test
import com.example.nsddemo.domain.engine.GameClient
import com.example.nsddemo.domain.engine.GameSession
import com.example.nsddemo.domain.model.GameData
import com.example.nsddemo.domain.model.GamePhase
import com.example.nsddemo.domain.model.Player
import com.example.nsddemo.presentation.util.MainDispatcherRule
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PauseViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: PauseViewModel
    private lateinit var mockGameSession: GameSession
    private lateinit var mockActiveClient: GameClient

    private val gameDataFlow = MutableStateFlow(GameData())
    private val gamePhaseFlow = MutableStateFlow<GamePhase>(GamePhase.Paused)

    @Before
    fun setUp() {
        mockGameSession = mockk(relaxed = true)
        mockActiveClient = mockk(relaxed = true)

        every { mockGameSession.gameData } returns gameDataFlow
        every { mockGameSession.gamePhase } returns gamePhaseFlow
        every { mockGameSession.activeClient } returns mockActiveClient

        viewModel = PauseViewModel(mockGameSession, UnconfinedTestDispatcher())
    }

    @Test
    fun `GIVEN game data WHEN players disconnect THEN filters disconnected players`() =
        runTest {
            gameDataFlow.value =
                GameData(
                    players =
                        mapOf(
                            "p1" to Player("Alice", "red", "p1", isConnected = true),
                            "p2" to Player("Bob", "blue", "p2", isConnected = false),
                        ),
                )
            viewModel.state.test {
                val updatedState = awaitItem()
                assertEquals(1, updatedState.disconnectedPlayers.size)
                assertEquals("Bob", updatedState.disconnectedPlayers.first().name)
                expectNoEvents()
            }
        }

    @Test
    fun `GIVEN End Game event WHEN triggered THEN disables button and calls client`() =
        runTest {
            assertTrue(viewModel.state.value.isEndGameButtonEnabled)

            viewModel.onEvent(PauseEvent.EndGame)
            advanceUntilIdle()

            viewModel.state.test {
                assertFalse(awaitItem().isEndGameButtonEnabled)
                coVerify(exactly = 1) { mockActiveClient.endGame() }
            }

            // Triggering again shouldn't fire a second network call
            viewModel.onEvent(PauseEvent.EndGame)
            advanceUntilIdle()
            coVerify(exactly = 1) { mockActiveClient.endGame() }
        }
}
