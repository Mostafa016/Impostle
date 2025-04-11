package com.example.nsddemo.presentation.screen.voting_results

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.nsddemo.core.util.GameState
import com.example.nsddemo.data.repository.GameRepository
import com.example.nsddemo.presentation.util.GameStateHandler
import com.example.nsddemo.presentation.util.Routes
import com.example.nsddemo.presentation.util.UiEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class VotingResultsViewModel(
    private val gameRepository: GameRepository,
    private val gameStateHandler: GameStateHandler
) : ViewModel() {
    val gameState = gameRepository.gameState
    val currentPlayer = gameRepository.gameData.value.currentPlayer!!
    val imposter = gameRepository.gameData.value.imposter!!
    val isImposter = gameRepository.gameData.value.isImposter!!
    val roundVotingCounts = gameRepository.gameData.value.roundVotingCounts

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        gameRepository.setAllowedStates(
            setOf(
                GameState.ShowScoreboard::class.simpleName!!,
                GameState.Transitioning::class.simpleName!!,
                GameState.Replay::class.simpleName!!, // For client to end game
                GameState.StartGame::class.simpleName!!, // For client to end game
            )
        )
        if (gameRepository.gameData.value.isHost!!) {
            viewModelScope.launch {
                gameStateHandler.handleGameStateChanges()
            }
        }
    }

    fun onEvent(event: VotingResultsEvent) {
        when (event) {
            VotingResultsEvent.ShowScores -> showScores()
            VotingResultsEvent.ReplayGame -> replayGame()
            VotingResultsEvent.EndGame -> endGame()
        }
    }

    private fun showScores() {
        gameRepository.updateGameState(GameState.ShowScoreboard)
        viewModelScope.launch {
            _eventFlow.emit(UiEvent.NavigateTo(Routes.Scoreboard.route))
        }
    }

    private fun replayGame() {
        resetRoundParameters()
        viewModelScope.launch {
            _eventFlow.emit(UiEvent.NavigateTo(Routes.Lobby.route))
        }
    }

    private fun resetRoundParameters() {
        gameRepository.updateGameState(
            GameState.Transitioning(
                gameRepository.gameState.value,
                GameState.StartNewRound
            )
        )
        gameRepository.updateGameData(
            gameRepository.gameData.value.copy(
                currentPlayerPairIndex = 0,
                roundPlayerPairs = emptyList(),
                roundPlayerVotes = emptyMap(),
                roundVotingCounts = emptyMap(),
                imposter = null,
                categoryOrdinal = -1,
                wordResID = -1,
                isFirstRound = false,
            )
        )
    }

    private fun endGame() {
        viewModelScope.launch {
            _eventFlow.emit(UiEvent.NavigateTo(Routes.EndGame.route))
        }
    }


    companion object {
        class VotingResultsViewModelFactory(
            private val gameRepository: GameRepository,
            private val gameStateHandler: GameStateHandler
        ) : ViewModelProvider.NewInstanceFactory() {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return VotingResultsViewModel(gameRepository, gameStateHandler) as T
            }
        }
    }
}