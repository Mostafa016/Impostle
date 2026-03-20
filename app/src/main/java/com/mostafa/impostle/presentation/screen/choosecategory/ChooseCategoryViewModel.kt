package com.mostafa.impostle.presentation.screen.choosecategory

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.mostafa.impostle.core.util.Debugging.TAG
import com.mostafa.impostle.di.IoDispatcher
import com.mostafa.impostle.domain.engine.GameSession
import com.mostafa.impostle.domain.model.GameCategory
import com.mostafa.impostle.presentation.util.BaseGameViewModel
import com.mostafa.impostle.presentation.util.Routes
import com.mostafa.impostle.presentation.util.UiEvent
import com.mostafa.impostle.presentation.util.uiCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
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
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
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
                withContext(ioDispatcher) {
                    activeClient?.selectCategory(selectedCategory)
                }
                _eventFlow.emit(UiEvent.NavigateTo(Routes.Lobby.route))
            }
        }
    }
