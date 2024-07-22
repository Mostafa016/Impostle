package com.example.nsddemo.presentation.screen.question

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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class QuestionViewModel(
    private val gameRepository: GameRepository, private val gameStateHandler: GameStateHandler
) : ViewModel() {
    private val gameData = gameRepository.gameData
    private val gameState = gameRepository.gameState

    private val currentPlayer = gameData.value.currentPlayer!!
    val categoryResID = gameData.value.category!!.nameResourceId
    val wordResID = gameData.value.wordResID
    val isImposter = gameData.value.isImposter!!

    private val _state = (gameState.value as GameState.AskQuestion).let { askQuestionGameState ->
        MutableStateFlow(
            QuestionState(
                isWordDialogVisible = false,
                askingPlayer = askQuestionGameState.asker,
                askedPlayer = askQuestionGameState.asked,
                isCurrentPlayerAsking = askQuestionGameState.asker == currentPlayer,
                isCurrentPlayerAsked = askQuestionGameState.asked == currentPlayer
            )
        )
    }
    val state = _state.asStateFlow()

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        gameRepository.setAllowedStates(
            setOf(
                GameState.AskQuestion::class.simpleName!!,
                GameState.ConfirmCurrentPlayerQuestion::class.simpleName!!,
                GameState.ChooseExtraQuestions::class.simpleName!!
            )
        )
        viewModelScope.launch(Dispatchers.IO) {
            if (gameRepository.gameData.value.isHost!!) {
                launch {
                    gameStateHandler.handleGameStateChanges()
                }
            }
            listenToGameStateChanges()
        }
    }

    fun onEvent(event: QuestionEvent) {
        when (event) {
            QuestionEvent.ShowWordDialog -> showWordDialog()
            QuestionEvent.DismissWordDialog -> dismissWordDialog()
            QuestionEvent.ConfirmWordDialog -> confirmWordDialog()
            QuestionEvent.FinishAskingYourQuestion -> finishAskingYourQuestion()
        }
    }

    private suspend fun listenToGameStateChanges() {
        gameState.collect { gameState ->
            when (gameState) {
                is GameState.AskQuestion -> {
                    _state.value = _state.value.copy(
                        askingPlayer = gameState.asker,
                        askedPlayer = gameState.asked,
                        isCurrentPlayerAsking = gameState.asker == currentPlayer,
                        isCurrentPlayerAsked = gameState.asked == currentPlayer
                    )
                }

                is GameState.ChooseExtraQuestions -> {
                    if (gameRepository.gameData.value.isHost!!) {
                        gameStateHandler.lastStateHandlerListener.first { it }
                    }
                    _eventFlow.emit(UiEvent.NavigateTo(Routes.ChooseExtraQuestions.route))
                }

                else -> {
                    // Do nothing
                }
            }
        }
    }

    private fun showWordDialog() {
        _state.value = _state.value.copy(isWordDialogVisible = true)
    }

    private fun dismissWordDialog() {
        _state.value = _state.value.copy(isWordDialogVisible = false)
    }

    private fun confirmWordDialog() {
        _state.value = _state.value.copy(isWordDialogVisible = false)
    }

    private fun finishAskingYourQuestion() {
        _state.value = state.value.copy(
            isDoneAskingQuestionClicked = true
        )
        (gameRepository.gameState.value as GameState.AskQuestion).also { currentAskQuestionState ->
            gameRepository.updateGameState(
                GameState.ConfirmCurrentPlayerQuestion(
                    currentAskQuestionState
                )
            )
        }
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        class QuestionViewModelFactory(
            private val gameRepository: GameRepository,
            private val gameStateHandler: GameStateHandler
        ) : ViewModelProvider.NewInstanceFactory() {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return QuestionViewModel(gameRepository, gameStateHandler) as T
            }
        }
    }
}