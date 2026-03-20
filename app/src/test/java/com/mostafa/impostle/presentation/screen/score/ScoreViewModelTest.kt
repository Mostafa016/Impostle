package com.mostafa.impostle.presentation.screen.score

import app.cash.turbine.test
import com.mostafa.impostle.R
import com.mostafa.impostle.domain.engine.GameClient
import com.mostafa.impostle.domain.engine.GameSession
import com.mostafa.impostle.domain.model.GameData
import com.mostafa.impostle.domain.model.GamePhase
import com.mostafa.impostle.presentation.util.MainDispatcherRule
import com.mostafa.impostle.presentation.util.UiEvent
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScoreViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `GIVEN phase changes to Lobby WHEN observed THEN emits snackbar`() =
        runTest {
            val mockGameSession = mockk<GameSession>(relaxed = true)
            val gamePhaseFlow = MutableStateFlow<GamePhase>(GamePhase.GameReplayChoice)

            every { mockGameSession.gameData } returns MutableStateFlow(GameData())
            every { mockGameSession.gamePhase } returns gamePhaseFlow

            val viewModel = ScoreViewModel(mockGameSession, UnconfinedTestDispatcher())

            viewModel.eventFlow.test {
                // Act: Server resets game
                gamePhaseFlow.value = GamePhase.Lobby
                advanceUntilIdle()

                val event = awaitItem() as UiEvent.ShowSnackBar
                assertEquals(R.string.continuing_playing, event.messageResId)
            }
        }

    @Test
    fun `WHEN host buttons clicked THEN client sends requests`() =
        runTest {
            val mockGameSession = mockk<GameSession>(relaxed = true)
            val mockActiveClient = mockk<GameClient>(relaxed = true)
            every { mockGameSession.gameData } returns MutableStateFlow(GameData())
            every { mockGameSession.gamePhase } returns MutableStateFlow(GamePhase.GameReplayChoice)
            every { mockGameSession.activeClient } returns mockActiveClient

            val viewModel = ScoreViewModel(mockGameSession, UnconfinedTestDispatcher())

            viewModel.onEvent(ScoreEvent.ReplayGame)
            advanceUntilIdle()
            coVerify(exactly = 1) { mockActiveClient.replayGame() }

            viewModel.onEvent(ScoreEvent.EndGame)
            advanceUntilIdle()
            coVerify(exactly = 1) { mockActiveClient.endGame() }
        }
}
