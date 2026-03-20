package com.mostafa.impostle.presentation.screen.rolereveal

import android.util.Log
import app.cash.turbine.test
import com.mostafa.impostle.domain.engine.GameClient
import com.mostafa.impostle.domain.engine.GameSession
import com.mostafa.impostle.domain.model.GameCategory
import com.mostafa.impostle.domain.model.GameData
import com.mostafa.impostle.domain.model.GamePhase
import com.mostafa.impostle.presentation.util.MainDispatcherRule
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertFalse

@OptIn(ExperimentalCoroutinesApi::class)
class RoleRevealViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Before
    fun setUp() {
        // Mock Log to prevent RuntimeExceptions in tests (as Log is static Android)
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
    }

    @Test
    fun `WHEN confirm clicked THEN state updates AND notifies client`() =
        runTest {
            val mockGameSession = mockk<GameSession>(relaxed = true)
            val mockActiveClient = mockk<GameClient>(relaxed = true)

            every { mockGameSession.gameData } returns MutableStateFlow(GameData(category = GameCategory.ANIMALS))
            every { mockGameSession.gamePhase } returns MutableStateFlow(GamePhase.RoleDistribution)
            every { mockGameSession.activeClient } returns mockActiveClient

            val viewModel = RoleRevealViewModel(mockGameSession, UnconfinedTestDispatcher())

            viewModel.state.test {
                assertFalse(awaitItem().isConfirmPressed)
                viewModel.onEvent(RoleRevealEvent.ConfirmRoleReveal)
                assertTrue(awaitItem().isConfirmPressed)
            }

            coVerify(exactly = 1) { mockActiveClient.confirmRole() }
        }
}
