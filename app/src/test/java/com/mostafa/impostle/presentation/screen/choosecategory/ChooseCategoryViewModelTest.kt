package com.mostafa.impostle.presentation.screen.choosecategory

import app.cash.turbine.test
import com.mostafa.impostle.domain.engine.GameClient
import com.mostafa.impostle.domain.engine.GameSession
import com.mostafa.impostle.domain.model.GameCategory
import com.mostafa.impostle.domain.model.GameData
import com.mostafa.impostle.domain.model.GamePhase
import com.mostafa.impostle.presentation.util.MainDispatcherRule
import com.mostafa.impostle.presentation.util.Routes
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
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChooseCategoryViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var mockGameSession: GameSession
    private lateinit var mockActiveClient: GameClient

    @Before
    fun setUp() {
        mockGameSession = mockk(relaxed = true)
        mockActiveClient = mockk(relaxed = true)
        every { mockGameSession.gameData } returns MutableStateFlow(GameData())
        every { mockGameSession.gamePhase } returns MutableStateFlow(GamePhase.Lobby)
        every { mockGameSession.activeClient } returns mockActiveClient
    }

    @Test
    fun `WHEN category chosen confirmed THEN notifies client AND navigates to Lobby`() =
        runTest {
            val viewModel = ChooseCategoryViewModel(mockGameSession, UnconfinedTestDispatcher())

            viewModel.eventFlow.test {
                viewModel.onEvent(ChooseCategoryEvent.CategoryChosen(GameCategory.FOOD))
                viewModel.onEvent(ChooseCategoryEvent.ConfirmSelection)
                advanceUntilIdle()

                coVerify(exactly = 1) { mockActiveClient.selectCategory(GameCategory.FOOD) }

                val event = awaitItem() as UiEvent.NavigateTo
                assertEquals(Routes.Lobby.route, event.destination)
            }
        }
}
