package com.example.nsddemo.presentation.screen.question

import com.example.nsddemo.domain.engine.GameClient
import com.example.nsddemo.domain.engine.GameSession
import com.example.nsddemo.domain.model.GameCategory
import com.example.nsddemo.domain.model.GameData
import com.example.nsddemo.domain.model.GamePhase
import com.example.nsddemo.domain.model.Player
import com.example.nsddemo.domain.model.RoundData
import com.example.nsddemo.presentation.util.MainDispatcherRule
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class QuestionViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var mockGameSession: GameSession
    private lateinit var mockActiveClient: GameClient
    private lateinit var gameDataFlow: MutableStateFlow<GameData>
    private lateinit var gamePhaseFlow: MutableStateFlow<GamePhase>

    @Before
    fun setUp() {
        mockGameSession = mockk(relaxed = true)
        mockActiveClient = mockk(relaxed = true)

        // Setup initial static data BEFORE ViewModel creation
        gameDataFlow =
            MutableStateFlow(
                GameData(
                    category = GameCategory.ANIMALS,
                    word = "Lion",
                    imposterId = "imposter_uuid",
                    localPlayerId = "local_uuid",
                    players =
                        mapOf(
                            "local_uuid" to Player("Alice", "red", "local_uuid"),
                            "imposter_uuid" to Player("Bob", "blue", "imposter_uuid"),
                        ),
                ),
            )
        gamePhaseFlow = MutableStateFlow(GamePhase.InRound)

        every { mockGameSession.gameData } returns gameDataFlow
        every { mockGameSession.gamePhase } returns gamePhaseFlow
        every { mockGameSession.activeClient } returns mockActiveClient
    }

    @Test
    fun `GIVEN innocent player WHEN initialized THEN static properties are correct`() {
        val viewModel = QuestionViewModel(mockGameSession)

        assertEquals("lion", viewModel.word)
        assertFalse(viewModel.isImposter)
    }

    @Test
    fun `GIVEN round data changes WHEN observed THEN updates UI turn state`() =
        runTest {
            val viewModel = QuestionViewModel(mockGameSession)

            // Act: Server sends new Question Round Data
            gameDataFlow.value =
                gameDataFlow.value.copy(
                    roundData =
                        RoundData.QuestionRoundData(
                            roundPairs = listOf("local_uuid" to "imposter_uuid"),
                            currentPairIndex = 0,
                        ),
                )
            advanceUntilIdle()

            // Assert
            val state = viewModel.state.value
            assertEquals("Alice", state.askingPlayer.name)
            assertEquals("Bob", state.askedPlayer.name)
            assertTrue(state.isCurrentPlayerAsking)
            assertFalse(state.isCurrentPlayerAsked)
        }

    @Test
    fun `GIVEN asking player WHEN finishes turn THEN triggers client action`() =
        runTest {
            val viewModel = QuestionViewModel(mockGameSession)

            viewModel.onEvent(QuestionEvent.FinishAskingYourQuestion)
            advanceUntilIdle()

            assertTrue(viewModel.state.value.isDoneAskingQuestionClicked)
            coVerify { mockActiveClient.endTurn() }
        }
}
