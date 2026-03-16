package com.example.nsddemo.presentation.screen.votingresults

import com.example.nsddemo.domain.engine.GameClient
import com.example.nsddemo.domain.engine.GameSession
import com.example.nsddemo.domain.model.GameData
import com.example.nsddemo.domain.model.GamePhase
import com.example.nsddemo.domain.model.Player
import com.example.nsddemo.presentation.util.MainDispatcherRule
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
