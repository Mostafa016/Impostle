package com.example.nsddemo.presentation.screen.choosecategory

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.nsddemo.core.util.Debugging.TAG
import com.example.nsddemo.domain.engine.GameSession
import com.example.nsddemo.domain.model.GameCategory
import com.example.nsddemo.presentation.util.BaseGameViewModel
import com.example.nsddemo.presentation.util.Routes
import com.example.nsddemo.presentation.util.UiEvent
import com.example.nsddemo.presentation.util.uiCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ChooseCategoryViewModel
    @Inject
    constructor(
        gameSession: GameSession,
    ) : BaseGameViewModel(gameSession) {
        private val _eventFlow = MutableSharedFlow<UiEvent>()
        val eventFlow = _eventFlow.asSharedFlow()

        private val _selectedCategory =
            MutableStateFlow(
                gameSession.gameData.value.category
                    ?.uiCategory,
            )
        val selectedCategory = _selectedCategory.asStateFlow()

        fun onEvent(event: ChooseCategoryEvent) {
            when (event) {
                is ChooseCategoryEvent.CategoryChosen -> onChooseCategory(event.category)
                is ChooseCategoryEvent.ConfirmSelection -> onSelectionConfirmed()
            }
        }

        override fun onCleared() {
            super.onCleared()
            Log.i(TAG, "ChooseCategoryViewModel: Cleared!")
        }

        private fun onChooseCategory(domainCategory: GameCategory) {
            _selectedCategory.value = domainCategory.uiCategory
        }

        private fun onSelectionConfirmed() {
            val selectedCategory = selectedCategory.value?.domainCategory ?: return
            viewModelScope.launch {
                withContext(Dispatchers.IO) {
                    activeClient?.selectCategory(selectedCategory)
                }
                _eventFlow.emit(UiEvent.NavigateTo(Routes.Lobby.route))
            }
        }
    }
