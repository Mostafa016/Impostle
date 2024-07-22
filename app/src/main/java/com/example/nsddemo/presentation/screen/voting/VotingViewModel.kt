package com.example.nsddemo.presentation.screen.voting

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.nsddemo.core.util.Debugging.TAG
import com.example.nsddemo.core.util.GameState
import com.example.nsddemo.data.repository.GameRepository
import com.example.nsddemo.presentation.util.GameStateHandler
import com.example.nsddemo.presentation.util.Routes
import com.example.nsddemo.presentation.util.UiEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class VotingViewModel(
    private val gameRepository: GameRepository, private val gameStateHandler: GameStateHandler,
) : ViewModel() {
    private val isHost = gameRepository.gameData.value.isHost!!
    val gameState = gameRepository.gameState
    val playersExcludingCurrent = gameRepository.gameData.value.playersExcludingCurrent

    private val _state = MutableStateFlow(VotingState())
    val state = _state.asStateFlow()

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        gameRepository.setAllowedStates(
            setOf(
                GameState.GetCurrentPlayerVote::class.simpleName!!,
                GameState.GetPlayerVote::class.simpleName!!,
                GameState.EndVote::class.simpleName!!
            )
        )
        viewModelScope.launch(Dispatchers.IO) {
            if (isHost) {
                launch {
                    Log.d(TAG, "Server Handling game state changes")
                    gameStateHandler.handleGameStateChanges()
                }
            }
            listenToGameStateChanges()
        }
    }

    fun onEvent(event: VotingEvent) {
        when (event) {
            is VotingEvent.onVotedForPlayer -> {
                _state.value = state.value.copy(votedPlayer = event.player)
            }

            VotingEvent.onVoteConfirmed -> {
                if (!isHost) {
                    val gameData = gameRepository.gameData
                    gameRepository.updateGameData(
                        gameData.value.copy(
                            roundPlayerVotes = gameData.value.roundPlayerVotes + (gameData.value.currentPlayer!! to state.value.votedPlayer!!)
                        )
                    )
                }
                _state.value = state.value.copy(isVoteConfirmed = true)
                val votedPlayer = state.value.votedPlayer!!
                gameRepository.updateGameState(GameState.GetCurrentPlayerVote(votedPlayer))
            }
        }
    }

    private suspend fun listenToGameStateChanges() {
        // This is done because the state is updated in the handleServerMessages function
        // (in case of client) and in handleGetPlayerVoteState function (in case of host)
        // which means that the screen is not aware of that update
        Log.d(TAG, "Listening to end vote state")
        gameRepository.gameState.collect {
            when (it) {
                is GameState.EndVote -> {
                    if (isHost) {
                        gameStateHandler.lastStateHandlerListener.first { isLastStateHandled -> isLastStateHandled }
                    }
                    _eventFlow.emit(UiEvent.NavigateTo(Routes.VotingResults.route))
                }

                else -> {
                    // Do nothing
                }
            }
        }
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        class VotingViewModelFactory(
            private val gameRepository: GameRepository,
            private val gameStateHandler: GameStateHandler,
        ) : ViewModelProvider.NewInstanceFactory() {
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                VotingViewModel(gameRepository, gameStateHandler) as T
        }
    }
}