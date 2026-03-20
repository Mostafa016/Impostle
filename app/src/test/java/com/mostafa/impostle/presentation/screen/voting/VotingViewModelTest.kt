package com.mostafa.impostle.presentation.screen.voting

import com.mostafa.impostle.domain.engine.GameClient
import com.mostafa.impostle.domain.engine.GameSession
import com.mostafa.impostle.domain.model.GameData
import com.mostafa.impostle.domain.model.GamePhase
import com.mostafa.impostle.domain.model.Player
import com.mostafa.impostle.presentation.util.MainDispatcherRule
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VotingViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `WHEN player selected and vote confirmed THEN updates state and notifies client`() =
        runTest {
            val mockGameSession = mockk<GameSession>(relaxed = true)
            val mockActiveClient = mockk<GameClient>(relaxed = true)

            val p2 = Player("Bob", "blue", "p2")
            every { mockGameSession.gameData } returns MutableStateFlow(GameData(players = mapOf("p2" to p2)))
            every { mockGameSession.gamePhase } returns MutableStateFlow(GamePhase.GameVoting)
            every { mockGameSession.activeClient } returns mockActiveClient

            val viewModel = VotingViewModel(mockGameSession)

            // 1. Select player
            viewModel.onEvent(VotingEvent.VoteForPlayer(p2))
            assertEquals(p2, viewModel.state.value.votedPlayer)

            // 2. Confirm vote
            viewModel.onEvent(VotingEvent.VoteConfirmed)
            advanceUntilIdle()

            assertTrue(viewModel.state.value.isVoteConfirmed)
            coVerify { mockActiveClient.submitVote("p2") }
        }
}
