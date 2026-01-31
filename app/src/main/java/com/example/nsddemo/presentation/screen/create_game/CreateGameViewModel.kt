package com.example.nsddemo.presentation.screen.create_game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nsddemo.R
import com.example.nsddemo.domain.engine.GameSession
import com.example.nsddemo.domain.logic.GameCodeGenerator
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
class CreateGameViewModel @Inject constructor(
    private val sessionController: SessionController,
    private val gameSession: GameSession,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow<GameCreationState>(GameCreationState.InProgress)
    val state = _state.asStateFlow()

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        createGame()
    }

    private fun createGame() {
        viewModelScope.launch(Dispatchers.IO) {
            val gameCode = GameCodeGenerator.generate()
            val settings = settingsRepository.userSettings.first()

            // 1. Tell Service to start Host Mode
            sessionController.startHost(gameCode, settings.playerId)

            // 2. Wait for Session to be Ready
            gameSession.sessionState.collect { state ->
                when (state) {
                    is SessionState.Running -> {
                        // 3. Register the Host as a Player
                        val activeClient = gameSession.activeClient
                        if (activeClient != null) {
                            activeClient.registerPlayer(settings.playerName!!, settings.playerId)
                            try {
                                withTimeout(2_500L) {
                                    gameSession.gameData.first { it.players.isNotEmpty() }
                                    withContext(Dispatchers.Main.immediate) {
                                        _state.value = GameCreationState.Success
                                    }
                                }
                            } catch (e: TimeoutCancellationException) {
                                withContext(Dispatchers.Main.immediate) {
                                    _state.value =
                                        GameCreationState.Error("Couldn't complete handshake")
                                }
                            }
                        }
                    }

                    is SessionState.Error -> {
                        // Handle error (e.g. Port in use)
                        // For now, maybe just log or navigate back
                    }

                    else -> {} // Idle/Connecting
                }
            }
        }
    }

    fun onEvent(event: CreateGameEvent) {
        when (event) {
            CreateGameEvent.GameCreated -> {
                viewModelScope.launch {
                    _eventFlow.emit(UiEvent.NavigateTo(Routes.Lobby.route))
                }
            }

            CreateGameEvent.GameCreationFailed -> {
                viewModelScope.launch {
                    _eventFlow.emit(UiEvent.ShowSnackBar(R.string.couldn_t_create_game_try_again_later))
                    _eventFlow.emit(UiEvent.NavigateTo(Routes.MainMenu.route))
                }
            }
        }
    }
}