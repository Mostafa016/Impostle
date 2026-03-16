package com.example.nsddemo.presentation.screen.lobby

import app.cash.turbine.test
import com.example.nsddemo.domain.engine.GameClient
import com.example.nsddemo.domain.engine.GameSession
import com.example.nsddemo.domain.model.GameCategory
import com.example.nsddemo.domain.model.GameData
import com.example.nsddemo.domain.model.GamePhase
import com.example.nsddemo.domain.model.Player
import com.example.nsddemo.presentation.util.MainDispatcherRule
import com.example.nsddemo.presentation.util.Routes
import com.example.nsddemo.presentation.util.UiEvent
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LobbyViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: LobbyViewModel
    private lateinit var mockGameSession: GameSession
    private lateinit var mockActiveClient: GameClient

    private val gameDataFlow =
        MutableStateFlow(GameData(gameCode = "ABCD", hostId = "local", localPlayerId = "local"))
    private val gamePhaseFlow = MutableStateFlow<GamePhase>(GamePhase.Lobby)

    @Before
    fun setUp() {
        mockGameSession = mockk(relaxed = true)
        mockActiveClient = mockk(relaxed = true)

        every { mockGameSession.gameData } returns gameDataFlow
        every { mockGameSession.gamePhase } returns gamePhaseFlow
        every { mockGameSession.activeClient } returns mockActiveClient

        viewModel = LobbyViewModel(mockGameSession)
    }

    @Test
    fun `GIVEN 1 player OR no category WHEN state updates THEN start button disabled`() =
        runTest {
            // Arrange: 1 Player, No Category
            viewModel.state.test {
                assertFalse(awaitItem().isStartRoundButtonEnabled) // Initial state

                gameDataFlow.value =
                    gameDataFlow.value.copy(
                        players = mapOf("local" to Player("Alice", "red", "local")),
                    )
                assertFalse(awaitItem().isStartRoundButtonEnabled)

                // Arrange: 2 Players, No Category
                gameDataFlow.value =
                    gameDataFlow.value.copy(
                        players =
                            mapOf(
                                "local" to Player("Alice", "red", "local"),
                                "p2" to Player("Bob", "blue", "p2"),
                            ),
                    )
                assertFalse(awaitItem().isStartRoundButtonEnabled)
                expectNoEvents()
            }
        }

    @Test
    fun `GIVEN 2 players AND category WHEN state updates THEN start button enabled`() =
        runTest {
            gameDataFlow.value =
                gameDataFlow.value.copy(
                    players =
                        mapOf(
                            "local" to Player("Alice", "red", "local"),
                            "p2" to Player("Bob", "blue", "p2"),
                        ),
                    category = GameCategory.ANIMALS,
                )
            advanceUntilIdle()
            viewModel.state.test {
                val newState = awaitItem()
                assertTrue(newState.isStartRoundButtonEnabled)
                assertEquals(GameCategory.ANIMALS, newState.chosenCategory)
            }
        }

    @Test
    fun `GIVEN UI events WHEN triggered THEN delegates to client or navigation`() =
        runTest {
            viewModel.eventFlow.test {
                // Act: Navigate to category
                viewModel.onEvent(LobbyEvent.ChooseCategoryButtonClick)
                val navEvent = awaitItem() as UiEvent.NavigateTo
                assertEquals(Routes.ChooseCategory.route, navEvent.destination)

                // Act: Start Round
                viewModel.onEvent(LobbyEvent.StartRound)
                advanceUntilIdle()
                coVerify { mockActiveClient.startGame() }

                // Act: Kick Player
                viewModel.onEvent(LobbyEvent.KickPlayer("p2"))
                advanceUntilIdle()
                coVerify { mockActiveClient.kickPlayer("p2") }
            }
        }
}
