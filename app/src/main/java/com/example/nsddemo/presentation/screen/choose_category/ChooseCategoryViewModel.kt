package com.example.nsddemo.presentation.screen.choose_category

import androidx.lifecycle.viewModelScope
import com.example.nsddemo.domain.engine.GameSession
import com.example.nsddemo.domain.model.GameCategory
import com.example.nsddemo.presentation.util.BaseGameViewModel
import com.example.nsddemo.presentation.util.Routes
import com.example.nsddemo.presentation.util.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ChooseCategoryViewModel @Inject constructor(
    gameSession: GameSession
) : BaseGameViewModel(gameSession) {

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    fun onEvent(event: ChooseCategoryEvent) {
        when (event) {
            is ChooseCategoryEvent.CategoryChosen -> onChooseCategory(event.category)
        }
    }

    private fun onChooseCategory(domainCategory: GameCategory) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                activeClient?.selectCategory(domainCategory)
            }
            _eventFlow.emit(UiEvent.NavigateTo(Routes.Lobby.route))
        }
    }
}