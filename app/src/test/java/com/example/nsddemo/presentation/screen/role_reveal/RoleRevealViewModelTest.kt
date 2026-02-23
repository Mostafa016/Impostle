package com.example.nsddemo.presentation.screen.role_reveal

import com.example.nsddemo.domain.engine.GameClient
import com.example.nsddemo.domain.engine.GameSession
import com.example.nsddemo.domain.model.GameCategory
import com.example.nsddemo.domain.model.GameData
import com.example.nsddemo.domain.model.GamePhase
import com.example.nsddemo.presentation.util.MainDispatcherRule
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RoleRevealViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `WHEN confirm clicked THEN state updates AND notifies client`() = runTest {
        val mockGameSession = mockk<GameSession>(relaxed = true)
        val mockActiveClient = mockk<GameClient>(relaxed = true)

        every { mockGameSession.gameData } returns MutableStateFlow(GameData(category = GameCategory.ANIMALS))
        every { mockGameSession.gamePhase } returns MutableStateFlow(GamePhase.RoleDistribution)
        every { mockGameSession.activeClient } returns mockActiveClient

        val viewModel = RoleRevealViewModel(mockGameSession)

        viewModel.onEvent(RoleRevealEvent.ConfirmRoleReveal)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.isConfirmPressed)
        coVerify(exactly = 1) { mockActiveClient.confirmRole() }
    }
}