package com.example.nsddemo.presentation.screen.join_game

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nsddemo.R
import com.example.nsddemo.core.util.Debugging.TAG
import com.example.nsddemo.core.util.GameConstants
import com.example.nsddemo.domain.engine.GameSession
import com.example.nsddemo.domain.model.SessionState
import com.example.nsddemo.domain.repository.SettingsRepository
import com.example.nsddemo.presentation.service.SessionController
import com.example.nsddemo.presentation.util.Routes
import com.example.nsddemo.presentation.util.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

@HiltViewModel
class JoinGameViewModel @Inject constructor(
    private val sessionController: SessionController,
    private val gameSession: GameSession,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(JoinGameState())
    val state = _state.asStateFlow()

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    fun onEvent(event: JoinGameEvent) {
        when (event) {
            is JoinGameEvent.GameCodeTextFieldValueChange -> onGameCodeTextFieldValueChange(event.text)
            JoinGameEvent.JoinGame -> onJoinGamePressed()
            JoinGameEvent.GoBackToMainMenu -> onGoBackToMainMenu()
        }
    }

    private fun onGameCodeTextFieldValueChange(text: String) {
        if (text.length > GameConstants.CODE_LENGTH) return
        _state.value = state.value.copy(
            gameCodeTextFieldText = text.uppercase()
        )
    }

    private fun onJoinGamePressed() {
        val gameCode = _state.value.gameCodeTextFieldText
        if (gameCode.length != GameConstants.CODE_LENGTH) return

        _state.value = state.value.copy(gameCodeTextFieldEnabled = false)

        viewModelScope.launch(Dispatchers.IO) {
            // 1. Start the Service in Join Mode
            val settings = settingsRepository.userSettings.first()
            sessionController.startJoin(gameCode, settings.playerId)

            // 2. Navigate to Loading Screen immediately
            _eventFlow.emit(UiEvent.NavigateTo(Routes.JoinGameLoading.route))

            // 3. Observe Session State for Success/Failure
            gameSession.sessionState.collect { sessionState ->
                when (sessionState) {
                    is SessionState.Running -> {
                        // Game Joined! Register Player.
                        val activeClient = gameSession.activeClient
                        if (activeClient != null) {
                            activeClient.registerPlayer(settings.playerName!!, settings.playerId)
                            try {
                                withTimeout(30_000L) {
                                    gameSession.gameData.first { it.players.isNotEmpty() }
                                    withContext(Dispatchers.Main.immediate) {
                                        _eventFlow.emit(UiEvent.ShowSnackBar(R.string.game_found_joining))
                                        _eventFlow.emit(UiEvent.NavigateTo(Routes.Lobby.route))
                                    }
                                }
                            } catch (e: TimeoutCancellationException) {
                                withContext(Dispatchers.Main.immediate) {
                                    Log.d(TAG, "JoinGameViewModel: Timed out")
                                    onGameSearchTimedOut()
                                }
                            }
                        }
                    }

                    is SessionState.Error -> {
                        _eventFlow.emit(UiEvent.ShowSnackBar(R.string.game_not_found))
                        // Go back to input
                        _state.value = state.value.copy(gameCodeTextFieldEnabled = true)
                        _eventFlow.emit(UiEvent.NavigateTo(Routes.JoinGame.route))
                    }

                    else -> {} // Connecting/Idle
                }
            }
        }
    }

    private fun onGoBackToMainMenu() {
        sessionController.stopSession()
        viewModelScope.launch {
            _eventFlow.emit(UiEvent.NavigateTo(Routes.MainMenu.route))
        }
    }

    private suspend fun onGameSearchTimedOut() {
        sessionController.stopSession()
        _state.value = JoinGameState()
        _eventFlow.emit(UiEvent.ShowSnackBar(R.string.game_not_found))
        _eventFlow.emit(UiEvent.NavigateTo(Routes.JoinGame.route))
    }
}