package com.example.nsddemo.presentation.screen.end_game

import androidx.lifecycle.viewModelScope
import com.example.nsddemo.domain.engine.GameSession
import com.example.nsddemo.presentation.service.SessionController
import com.example.nsddemo.presentation.util.BaseGameViewModel
import com.example.nsddemo.presentation.util.Routes
import com.example.nsddemo.presentation.util.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EndGameViewModel @Inject constructor(
    gameSession: GameSession,
    private val sessionController: SessionController
) : BaseGameViewModel(gameSession) {
    private val _state = MutableStateFlow(EndGameState())
    val state = _state.asStateFlow()

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    fun onEvent(event: EndGameEvent) {
        when (event) {
            EndGameEvent.EndGame -> quitToMainMenu()
        }
    }

    private fun quitToMainMenu() {
        _state.value = state.value.copy(isGoToMainMenuButtonEnabled = false)
        sessionController.stopSession()
        viewModelScope.launch {
            _eventFlow.emit(UiEvent.NavigateTo(Routes.MainMenu.route))
        }
    }
}