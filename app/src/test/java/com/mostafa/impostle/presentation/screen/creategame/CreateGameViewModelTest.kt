package com.mostafa.impostle.presentation.screen.creategame

import android.util.Log
import app.cash.turbine.test
import com.mostafa.impostle.R
import com.mostafa.impostle.domain.engine.GameClient
import com.mostafa.impostle.domain.engine.GameSession
import com.mostafa.impostle.domain.model.SessionState
import com.mostafa.impostle.presentation.fakes.FakeSettingsRepository
import com.mostafa.impostle.presentation.service.SessionController
import com.mostafa.impostle.presentation.util.MainDispatcherRule
import com.mostafa.impostle.presentation.util.Routes
import com.mostafa.impostle.presentation.util.UiEvent
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
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CreateGameViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var mockSessionController: SessionController
    private lateinit var mockGameSession: GameSession
    private lateinit var mockActiveClient: GameClient
    private lateinit var fakeSettingsRepo: FakeSettingsRepository

    private val sessionStateFlow = MutableStateFlow<SessionState>(SessionState.Connecting)

    @Before
    fun setUp() {
        // Mock Log to prevent RuntimeExceptions in tests (as Log is static Android)
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0

        mockSessionController = mockk(relaxed = true)
        mockGameSession = mockk(relaxed = true)
        mockActiveClient = mockk(relaxed = true)
        fakeSettingsRepo = FakeSettingsRepository("HostPlayer")

        every { mockGameSession.sessionState } returns sessionStateFlow
        every { mockGameSession.activeClient } returns mockActiveClient
    }

    @Test
    fun `GIVEN init WHEN session connects THEN sets success and registers player`() =
        runTest {
            // Arrange & Act
            val viewModel =
                CreateGameViewModel(
                    mockSessionController,
                    mockGameSession,
                    fakeSettingsRepo,
                    UnconfinedTestDispatcher(),
                )
            // Assert Controller called
            verify { mockSessionController.startHost("AAAA", "test-uuid-1234") }

            // Simulate Success
            sessionStateFlow.value = SessionState.Running
            advanceUntilIdle()

            assertEquals(GameCreationState.Success, viewModel.state.value)
            coVerify { mockActiveClient.registerPlayer("HostPlayer", "test-uuid-1234") }
        }

    @Test
    fun `GIVEN init WHEN connection times out THEN sets error state`() =
        runTest {
            val viewModel =
                CreateGameViewModel(
                    mockSessionController,
                    mockGameSession,
                    fakeSettingsRepo,
                    UnconfinedTestDispatcher(),
                )

            // Fast forward past the 30-second timeout
            advanceTimeBy(30_001L)
            advanceUntilIdle()

            assertEquals(GameCreationState.Error, viewModel.state.value)
        }

    @Test
    fun `GIVEN failure state WHEN GameCreationFailed event THEN stops session and navigates`() =
        runTest {
            val viewModel =
                CreateGameViewModel(
                    mockSessionController,
                    mockGameSession,
                    fakeSettingsRepo,
                    UnconfinedTestDispatcher(),
                )

            viewModel.eventFlow.test {
                viewModel.onEvent(CreateGameEvent.GameCreationFailed)
                advanceUntilIdle()

                verify { mockSessionController.stopSession() }

                val snackbarEvent = awaitItem() as UiEvent.ShowSnackBar
                assertEquals(
                    R.string.couldn_t_create_game_try_again_later,
                    snackbarEvent.messageResId,
                )

                val navEvent = awaitItem() as UiEvent.NavigateTo
                assertEquals(Routes.MainMenu.route, navEvent.destination)
            }
        }
}
