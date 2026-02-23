package com.example.nsddemo.presentation.screen.join_game

import android.util.Log
import app.cash.turbine.test
import com.example.nsddemo.R
import com.example.nsddemo.domain.engine.GameClient
import com.example.nsddemo.domain.engine.GameSession
import com.example.nsddemo.domain.model.SessionState
import com.example.nsddemo.presentation.fakes.FakeSettingsRepository
import com.example.nsddemo.presentation.service.SessionController
import com.example.nsddemo.presentation.util.MainDispatcherRule
import com.example.nsddemo.presentation.util.Routes
import com.example.nsddemo.presentation.util.UiEvent
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class JoinGameViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: JoinGameViewModel
    private lateinit var mockSessionController: SessionController
    private lateinit var mockGameSession: GameSession
    private lateinit var mockActiveClient: GameClient
    private lateinit var fakeSettingsRepo: FakeSettingsRepository

    // Control the flow of GameSession state during tests
    private val sessionStateFlow = MutableStateFlow<SessionState>(SessionState.Idle)

    @Before
    fun setUp() {
        mockSessionController = mockk(relaxed = true)
        mockGameSession = mockk(relaxed = true)
        mockActiveClient = mockk(relaxed = true)
        fakeSettingsRepo = FakeSettingsRepository("TestPlayer")

        every { mockGameSession.sessionState } returns sessionStateFlow
        every { mockGameSession.activeClient } returns mockActiveClient

        viewModel = JoinGameViewModel(
            mockSessionController,
            mockGameSession,
            fakeSettingsRepo,
            UnconfinedTestDispatcher()
        )

        // Mock Log to prevent RuntimeExceptions in tests (as Log is static Android)
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
    }

    @Test
    fun `GIVEN valid 4-letter code WHEN typing THEN enable join button`() = runTest {
        viewModel.onEvent(JoinGameEvent.GameCodeTextFieldValueChange("AB"))
        assertFalse(viewModel.state.value.isJoinGameButtonEnabled)

        viewModel.onEvent(JoinGameEvent.GameCodeTextFieldValueChange("ABCD"))
        assertTrue(viewModel.state.value.isJoinGameButtonEnabled)
        assertEquals("ABCD", viewModel.state.value.gameCodeTextFieldText)
    }

    @Test
    fun `GIVEN happy path WHEN join clicked THEN navigates to loading and starts session`() =
        runTest {
            // Arrange: valid code
            viewModel.onEvent(JoinGameEvent.GameCodeTextFieldValueChange("ABCD"))

            viewModel.eventFlow.test {
                // Act
                viewModel.onEvent(JoinGameEvent.JoinGame)
                runCurrent() // Allow coroutine to reach suspension point

                // Assert: Emits Navigate to Loading immediately
                val navEvent = awaitItem() as UiEvent.NavigateTo
                assertEquals(Routes.JoinGameLoading.route, navEvent.destination)

                // Assert: calls SessionController
                verify { mockSessionController.startJoin("ABCD", "test-uuid-1234") }

                // Simulate Successful Connection
                sessionStateFlow.value = SessionState.Running
                advanceUntilIdle()

                // Assert: Emits success snackbar
                val snackbarEvent = awaitItem() as UiEvent.ShowSnackBar
                assertEquals(R.string.game_found_joining, snackbarEvent.messageResId)

                // Assert: Registers Player
                coVerify { mockActiveClient.registerPlayer("TestPlayer", "test-uuid-1234") }
            }
        }

    @Test
    fun `GIVEN joining WHEN timeout reached THEN stops session and shows error`() = runTest {
        viewModel.onEvent(JoinGameEvent.GameCodeTextFieldValueChange("ABCD"))

        viewModel.eventFlow.test {
            viewModel.onEvent(JoinGameEvent.JoinGame)
            runCurrent()

            // Assert: Emits Navigate to Loading immediately
            val navEvent = awaitItem() as UiEvent.NavigateTo
            assertEquals(Routes.JoinGameLoading.route, navEvent.destination)

            // Assert: calls SessionController
            verify { mockSessionController.startJoin("ABCD", "test-uuid-1234") }

            // Act: Fast-forward virtual time past the 15_000L timeout WITHOUT changing sessionStateFlow
            advanceTimeBy(15_001L)

            // Assert: Stops session
            verify { mockSessionController.stopSession() }

            // Assert: Emits Error Snackbar
            val errorSnackbar = awaitItem() as UiEvent.ShowSnackBar
            assertEquals(R.string.game_not_found, errorSnackbar.messageResId)

            // Assert: Navigates back to JoinGame input
            val navBackEvent = awaitItem() as UiEvent.NavigateTo
            assertEquals(Routes.JoinGame.route, navBackEvent.destination)
        }
    }
}