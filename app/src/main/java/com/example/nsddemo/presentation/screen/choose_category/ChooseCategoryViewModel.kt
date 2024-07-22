package com.example.nsddemo.presentation.screen.choose_category

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.nsddemo.core.util.Debugging.TAG
import com.example.nsddemo.core.util.GameState
import com.example.nsddemo.data.repository.GameRepository
import com.example.nsddemo.domain.util.Categories
import com.example.nsddemo.presentation.util.GameStateHandler
import com.example.nsddemo.presentation.util.Routes
import com.example.nsddemo.presentation.util.UiEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class ChooseCategoryViewModel(
    private val gameRepository: GameRepository, private val gameStateHandler: GameStateHandler
) : ViewModel() {
    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        gameRepository.setAllowedStates(
            setOf(
                GameState.GetPlayerInfo::class.simpleName!!,
            )
        )
        viewModelScope.launch {
            gameStateHandler.handleGameStateChanges()
        }
    }

    fun onEvent(event: ChooseCategoryEvent) {
        when (event) {
            is ChooseCategoryEvent.CategoryChosen -> onChooseCategory(event.category)
        }
    }


    private fun onChooseCategory(category: Categories) {
        Log.d(TAG, "Chosen category: $category")
        gameRepository.updateGameData(
            gameRepository.gameData.value.copy(categoryOrdinal = category.ordinal)
        )
        viewModelScope.launch {
            _eventFlow.emit(UiEvent.NavigateTo(Routes.Lobby.route))
        }
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        class ChooseCategoryViewModelFactory(
            private val gameRepository: GameRepository,
            private val gameStateHandler: GameStateHandler
        ) : ViewModelProvider.NewInstanceFactory() {
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ChooseCategoryViewModel(gameRepository, gameStateHandler) as T
        }
    }
}