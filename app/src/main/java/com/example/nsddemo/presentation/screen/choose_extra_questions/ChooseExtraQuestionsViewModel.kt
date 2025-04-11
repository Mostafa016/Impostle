package com.example.nsddemo.presentation.screen.choose_extra_questions

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
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ChooseExtraQuestionsViewModel(
    private val gameRepository: GameRepository,
    private val gameStateHandler: GameStateHandler,
) : ViewModel() {
    val isHost = gameRepository.gameData.value.isHost!!
    val gameState = gameRepository.gameState

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        gameRepository.setAllowedStates(
            setOf(
                GameState.StartVote::class.simpleName!!,
                GameState.AskExtraQuestions::class.simpleName!!,
                GameState.AskQuestion::class.simpleName!!, // For server
            )
        )
        viewModelScope.launch(Dispatchers.IO) {
            if (isHost) {
                launch {
                    Log.d(TAG, "Server Handling game state changes")
                    gameStateHandler.handleGameStateChanges()
                }
            }
            // TODO: Should turn clientListenToGameStateChanges into a method that reacts to the
            //  game state similar to how handleGameStateChanges does (i.e. should just handle
            //  the game state changes the same way as the host (Message reader and
            //  game state updater, game state reactor))
            clientListenToGameStateChanges()
        }
    }

    fun onEvent(event: ChooseExtraQuestionsEvent) {
        when (event) {
            ChooseExtraQuestionsEvent.StartVote -> startVote()
            ChooseExtraQuestionsEvent.AskExtraQuestions -> askExtraQuestions()
        }
    }

    private suspend fun clientListenToGameStateChanges() {
        // This is done because the state is updated in the handleServerMessages function
        // which means that the screen is not aware of that update in case of clients
        if (isHost) {
            return
        }
        Log.d(TAG, "Client listening to game state changes")
        gameRepository.gameState.collect {
            when (it) {
                GameState.StartVote -> {
                    _eventFlow.emit(UiEvent.NavigateTo(Routes.Voting.route))
                }

                is GameState.AskQuestion -> {
                    _eventFlow.emit(UiEvent.NavigateTo(Routes.Question.route))
                }

                else -> {
                    // Do nothing
                }
            }
        }
    }

    private fun startVote() {
        gameRepository.updateGameState(GameState.StartVote)
        viewModelScope.launch {
            gameStateHandler.lastStateHandlerListener.first { isStateHandled -> isStateHandled }
            _eventFlow.emit(UiEvent.NavigateTo(Routes.Voting.route))
        }
    }

    private fun askExtraQuestions() {
        gameRepository.updateGameState(GameState.AskExtraQuestions)
        viewModelScope.launch {
            gameStateHandler.lastStateHandlerListener.first { isStateHandled -> isStateHandled }
            _eventFlow.emit(UiEvent.NavigateTo(Routes.Question.route))
        }
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        class ChooseExtraQuestionsViewModelFactory(
            private val gameRepository: GameRepository,
            private val gameStateHandler: GameStateHandler,
        ) : ViewModelProvider.NewInstanceFactory() {
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ChooseExtraQuestionsViewModel(gameRepository, gameStateHandler) as T
        }
    }
}