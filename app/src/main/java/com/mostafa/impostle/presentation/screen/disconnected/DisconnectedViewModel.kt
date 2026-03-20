package com.mostafa.impostle.presentation.screen.disconnected

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
class DisconnectedViewModel
    @Inject
    constructor(
        private val sessionController: SessionController,
    ) : ViewModel() {
        private val _state = MutableStateFlow(DisconnectedState())
        val state = _state.asStateFlow()

        private val _eventFlow = MutableSharedFlow<UiEvent>()
        val eventFlow = _eventFlow.asSharedFlow()

        fun onEvent(event: DisconnectedEvent) {
            when (event) {
                DisconnectedEvent.ReconnectButtonPressed -> {
                    reconnectToGame()
                }

                DisconnectedEvent.GoToMainMenuButtonPressed -> quitToMainMenu()
            }
        }

        override fun onCleared() {
            super.onCleared()
            Log.i(TAG, "DisconnectedViewModel: Cleared!")
        }

        private fun reconnectToGame() {
            if (!state.value.isReconnectButtonEnabled) return
            _state.value = state.value.copy(isReconnectButtonEnabled = false)
            sessionController.stopSession()
            viewModelScope.launch {
                _eventFlow.emit(UiEvent.NavigateTo(Routes.JoinGameGraph.route))
            }
        }

        private fun quitToMainMenu() {
            if (!state.value.isGoToMainMenuButtonEnabled) return
            _state.value = state.value.copy(isGoToMainMenuButtonEnabled = false)
            sessionController.stopSession()
            viewModelScope.launch {
                _eventFlow.emit(UiEvent.NavigateTo(Routes.MainMenu.route))
            }
        }
    }
