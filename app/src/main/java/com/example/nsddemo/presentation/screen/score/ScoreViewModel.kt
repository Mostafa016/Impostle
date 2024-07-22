package com.example.nsddemo.presentation.screen.score

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.nsddemo.core.util.GameState
import com.example.nsddemo.data.repository.GameRepository
import com.example.nsddemo.presentation.util.GameStateHandler
import com.example.nsddemo.presentation.util.Routes
import com.example.nsddemo.presentation.util.UiEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ScoreViewModel(
    private val gameRepository: GameRepository, private val gameStateHandler: GameStateHandler
) : ViewModel() {
    val gameState = gameRepository.gameState
    val isHost = gameRepository.gameData.value.isHost!!
    val currentPlayer = gameRepository.gameData.value.currentPlayer!!
    val imposter = gameRepository.gameData.value.imposter!!
    val isImposter = gameRepository.gameData.value.isImposter!!
    val playerScores = gameRepository.gameData.value.playerScores

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        gameRepository.setAllowedStates(
            setOf(
                GameState.Replay::class.simpleName!!,
                GameState.Transitioning::class.simpleName!!
            )
        )
        viewModelScope.launch(Dispatchers.IO) {
            gameStateHandler.handleGameStateChanges()
        }
    }

    fun onEvent(event: ScoreEvent) {
        when (event) {
            // TODO: Make two versions of each state change event, one for client side
            //  and one for server side for the rest of the screens that have similar structure
            ScoreEvent.ReplayGame -> replayGame()
            ScoreEvent.EndGame -> endGame()
            ScoreEvent.ReplayGameServerSide -> replayGameServerSide()
            ScoreEvent.EndGameServerSide -> endGameServerSide()
        }
    }

    private fun replayGame() {
        resetRoundParameters()
        viewModelScope.launch {
            if (isHost) {
                gameStateHandler.lastStateHandlerListener.first { it }
            }
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
            if (isHost) {
                gameStateHandler.lastStateHandlerListener.first { it }
            }
            _eventFlow.emit(UiEvent.NavigateTo(Routes.EndGame.route))
        }
    }

    private fun replayGameServerSide() {
        gameRepository.updateGameState(GameState.Replay(true))
    }

    private fun endGameServerSide() {
        gameRepository.updateGameState(GameState.Replay(false))
    }

    companion object {
        class ScoreViewModelFactory(
            private val gameRepository: GameRepository,
            private val gameStateHandler: GameStateHandler
        ) : ViewModelProvider.NewInstanceFactory() {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ScoreViewModel(gameRepository, gameStateHandler) as T
            }
        }
    }
}