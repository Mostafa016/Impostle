package com.mostafa.impostle.presentation.screen.votingresults

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
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VotingResultsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `WHEN host clicks show scores THEN requests continue to game choice`() =
        runTest {
            val mockGameSession = mockk<GameSession>(relaxed = true)
            val mockActiveClient = mockk<GameClient>(relaxed = true)

            val me = Player("Me", "red", "me")
            every { mockGameSession.gameData } returns
                MutableStateFlow(
                    GameData(
                        localPlayerId = "me",
                        players = mapOf("me" to me),
                        imposterId = "me",
                    ),
                )
            every { mockGameSession.gamePhase } returns MutableStateFlow(GamePhase.GameResults)
            every { mockGameSession.activeClient } returns mockActiveClient

            val viewModel = VotingResultsViewModel(mockGameSession)

            viewModel.onEvent(VotingResultsEvent.ShowScores)
            advanceUntilIdle()

            coVerify(exactly = 1) { mockActiveClient.continueToGameChoice() }
        }
}
