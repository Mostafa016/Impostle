package com.example.nsddemo.presentation.screen.replayroundchoice

import com.example.nsddemo.domain.engine.GameClient
import com.example.nsddemo.domain.engine.GameSession
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
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReplayRoundChoiceViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `WHEN host choices made THEN buttons disable AND client notified`() =
        runTest {
            val mockGameSession = mockk<GameSession>(relaxed = true)
            val mockActiveClient = mockk<GameClient>(relaxed = true)
            every { mockGameSession.gameData } returns
                MutableStateFlow(
                    GameData(
                        hostId = "me",
                        localPlayerId = "me",
                    ),
                )
            every { mockGameSession.gamePhase } returns MutableStateFlow(GamePhase.RoundReplayChoice)
            every { mockGameSession.activeClient } returns mockActiveClient

            val viewModel = ReplayRoundChoiceViewModel(mockGameSession)

            // Action 1
            viewModel.onEvent(ReplayRoundChoiceEvent.ReplayRound)
            advanceUntilIdle()
            assertFalse(viewModel.state.value.isReplayRoundButtonEnabled)
            coVerify { mockActiveClient.replayRound() }

            // Action 2
            viewModel.onEvent(ReplayRoundChoiceEvent.StartVote)
            advanceUntilIdle()
            assertFalse(viewModel.state.value.isStartVoteButtonEnabled)
            coVerify { mockActiveClient.startVote() }
        }
}
