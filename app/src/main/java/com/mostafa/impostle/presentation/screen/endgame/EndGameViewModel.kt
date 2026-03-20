package com.mostafa.impostle.presentation.screen.endgame

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mostafa.impostle.core.util.Debugging.TAG
import com.mostafa.impostle.presentation.service.SessionController
import com.mostafa.impostle.presentation.util.Routes
import com.mostafa.impostle.presentation.util.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EndGameViewModel
    @Inject
    constructor(
        private val sessionController: SessionController,
    ) : ViewModel() {
        private val _state = MutableStateFlow(EndGameState())
        val state = _state.asStateFlow()

        private val _eventFlow = MutableSharedFlow<UiEvent>()
        val eventFlow = _eventFlow.asSharedFlow()

        fun onEvent(event: EndGameEvent) {
            when (event) {
                EndGameEvent.EndGame -> quitToMainMenu()
            }
        }

        override fun onCleared() {
            super.onCleared()
            Log.i(TAG, "EndGameViewModel: Cleared!")
        }

        private fun quitToMainMenu() {
            _state.value = state.value.copy(isGoToMainMenuButtonEnabled = false)
            sessionController.stopSession()
            viewModelScope.launch {
                _eventFlow.emit(UiEvent.NavigateTo(Routes.MainMenu.route))
            }
        }
    }
