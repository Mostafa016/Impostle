package com.mostafa.impostle.presentation.screen.mainmenu

import app.cash.turbine.test
import com.mostafa.impostle.presentation.fakes.FakeSettingsRepository
import com.mostafa.impostle.presentation.util.MainDispatcherRule
import com.mostafa.impostle.presentation.util.Routes
import com.mostafa.impostle.presentation.util.UiEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainMenuViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: MainMenuViewModel
    private lateinit var fakeSettingsRepo: FakeSettingsRepository

    private fun setupViewModel(initialName: String? = null) {
        fakeSettingsRepo = FakeSettingsRepository(initialPlayerName = initialName)
        viewModel = MainMenuViewModel(fakeSettingsRepo, UnconfinedTestDispatcher())
    }

    @Test
    fun `GIVEN returning user WHEN initialized THEN loads player name from settings`() =
        runTest {
            setupViewModel(initialName = "VeteranPlayer")
            advanceUntilIdle() // Allow the init block coroutine to finish reading settings

            val state = viewModel.state.value
            println(state)
            assertEquals("VeteranPlayer", state.playerName)
            assertEquals("VeteranPlayer", state.playerNameTextFieldText)
        }

    @Test
    fun `GIVEN no player name WHEN create game clicked THEN shows name dialog`() =
        runTest {
            setupViewModel(initialName = null)
            advanceUntilIdle()

            viewModel.onEvent(MainMenuEvent.CreateGameClick)

            assertTrue(viewModel.state.value.isPlayerNameDialogVisible)
        }

    @Test
    fun `GIVEN player name exists WHEN create game clicked THEN navigates to CreateGame`() =
        runTest {
            setupViewModel(initialName = "ReadyPlayer")
            advanceUntilIdle()

            viewModel.eventFlow.test {
                viewModel.onEvent(MainMenuEvent.CreateGameClick)

                val event = awaitItem() as UiEvent.NavigateTo
                assertEquals(Routes.CreateGameLoading.route, event.destination)
            }
        }

    @Test
    fun `GIVEN name dialog open WHEN name is saved THEN updates repo AND dismisses dialog`() =
        runTest {
            setupViewModel(initialName = null)
            advanceUntilIdle()

            // Open dialog
            viewModel.onEvent(MainMenuEvent.PlayerNameClick)
            // Type name
            viewModel.onEvent(MainMenuEvent.PlayerNameDialogTextChange("NewName"))
            // Save
            viewModel.onEvent(MainMenuEvent.PlayerNameDialogSave("NewName"))
            advanceUntilIdle()

            val state = viewModel.state.value
            assertEquals("NewName", state.playerName)
            assertEquals("NewName", fakeSettingsRepo.userSettings.first().playerName)
            assertFalse(state.isPlayerNameDialogVisible)
        }
}
