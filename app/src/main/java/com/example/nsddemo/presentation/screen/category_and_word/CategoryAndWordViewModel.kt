package com.example.nsddemo.presentation.screen.category_and_word

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
import kotlinx.coroutines.launch

class CategoryAndWordViewModel(
    private val gameRepository: GameRepository,
    private val gameStateHandler: GameStateHandler
) :
    ViewModel() {
    val gameState = gameRepository.gameState

    private val _state = MutableStateFlow(CategoryAndWordState())
    val state = _state.asStateFlow()

    val isImposter = gameRepository.gameData.value.isImposter!!

    val categoryOrdinal: Int =
        (gameState.value as GameState.DisplayCategoryAndWord).categoryOrdinal
    val wordResourceId: Int =
        (gameState.value as GameState.DisplayCategoryAndWord).wordResourceId

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        gameRepository.setAllowedStates(
            setOf(
                GameState.GetPlayerReadCategoryAndWordConfirmation::class.simpleName!!,
                GameState.ConfirmCurrentPlayerReadCategoryAndWord::class.simpleName!!
            )
        )
        viewModelScope.launch(Dispatchers.IO) {
            if (gameRepository.gameData.value.isHost!!) {
                gameStateHandler.handleGameStateChanges()
            }
        }
    }

    fun onEvent(event: CategoryAndWordEvent) {
        when (event) {
            CategoryAndWordEvent.StartQuestions -> {
                onStartQuestions()
            }

            CategoryAndWordEvent.ConfirmCategoryAndWord -> {
                onConfirmClick()
            }
        }
    }

    private fun onStartQuestions() {
        viewModelScope.launch {
            _eventFlow.emit(UiEvent.NavigateTo(Routes.Question.route))
        }
    }

    private fun onConfirmClick() {
        _state.value = _state.value.copy(isConfirmPressed = true)
        when (val currentGameState = gameState.value) {
            is GameState.GetPlayerReadCategoryAndWordConfirmation -> {
                gameRepository.updateGameState(
                    GameState.ConfirmCurrentPlayerReadCategoryAndWord(currentGameState.numberOfConfirmations + 1)
                )
            }

            else -> {
                gameRepository.updateGameState(GameState.ConfirmCurrentPlayerReadCategoryAndWord(1))
            }
        }
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        class CategoryAndWordViewModelFactory(
            private val gameRepository: GameRepository,
            private val gameStateHandler: GameStateHandler
        ) :
            ViewModelProvider.NewInstanceFactory() {
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                CategoryAndWordViewModel(gameRepository, gameStateHandler) as T
        }
    }
}