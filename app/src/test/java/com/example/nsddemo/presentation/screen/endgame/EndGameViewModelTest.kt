package com.example.nsddemo.presentation.screen.endgame

import app.cash.turbine.test
import com.example.nsddemo.presentation.service.SessionController
import com.example.nsddemo.presentation.util.MainDispatcherRule
import com.example.nsddemo.presentation.util.Routes
import com.example.nsddemo.presentation.util.UiEvent
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EndGameViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `WHEN EndGame clicked THEN stops session and navigates to MainMenu`() =
        runTest {
            val mockSessionController = mockk<SessionController>(relaxed = true)
            val viewModel = EndGameViewModel(mockSessionController)

            viewModel.eventFlow.test {
                viewModel.onEvent(EndGameEvent.EndGame)
                advanceUntilIdle()

                verify(exactly = 1) { mockSessionController.stopSession() }
                assertFalse(viewModel.state.value.isGoToMainMenuButtonEnabled)

                val navEvent = awaitItem() as UiEvent.NavigateTo
                assertEquals(Routes.MainMenu.route, navEvent.destination)
            }
        }
}
