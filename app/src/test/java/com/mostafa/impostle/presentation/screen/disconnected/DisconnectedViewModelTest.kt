package com.mostafa.impostle.presentation.screen.disconnected

import app.cash.turbine.test
import com.mostafa.impostle.presentation.service.SessionController
import com.mostafa.impostle.presentation.util.MainDispatcherRule
import com.mostafa.impostle.presentation.util.Routes
import com.mostafa.impostle.presentation.util.UiEvent
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DisconnectedViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: DisconnectedViewModel
    private lateinit var mockSessionController: SessionController

    @Before
    fun setUp() {
        mockSessionController = mockk(relaxed = true)
        viewModel = DisconnectedViewModel(mockSessionController)
    }

    @Test
    fun `WHEN reconnect pressed THEN stops session, disables button, navigates to Join`() =
        runTest {
            viewModel.eventFlow.test {
                viewModel.onEvent(DisconnectedEvent.ReconnectButtonPressed)
                advanceUntilIdle()

                // 1. Session stopped
                verify { mockSessionController.stopSession() }

                // 2. Button disabled (prevents double clicking)
                assertFalse(viewModel.state.value.isReconnectButtonEnabled)

                // 3. Navigates correctly
                val navEvent = awaitItem() as UiEvent.NavigateTo
                assertEquals(Routes.JoinGameGraph.route, navEvent.destination)
            }
        }

    @Test
    fun `WHEN main menu pressed THEN stops session, disables button, navigates to MainMenu`() =
        runTest {
            viewModel.eventFlow.test {
                viewModel.onEvent(DisconnectedEvent.GoToMainMenuButtonPressed)
                advanceUntilIdle()

                verify { mockSessionController.stopSession() }
                assertFalse(viewModel.state.value.isGoToMainMenuButtonEnabled)

                val navEvent = awaitItem() as UiEvent.NavigateTo
                assertEquals(Routes.MainMenu.route, navEvent.destination)
            }
        }
}
