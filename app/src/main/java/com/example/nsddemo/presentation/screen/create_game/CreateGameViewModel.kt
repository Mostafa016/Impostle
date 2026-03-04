package com.example.nsddemo.presentation.screen.create_game

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nsddemo.R
import com.example.nsddemo.core.util.Debugging.TAG
import com.example.nsddemo.di.IoDispatcher
import com.example.nsddemo.domain.engine.GameSession
import com.example.nsddemo.domain.model.SessionState
import com.example.nsddemo.domain.repository.SettingsRepository
import com.example.nsddemo.presentation.service.SessionController
import com.example.nsddemo.presentation.util.Routes
import com.example.nsddemo.presentation.util.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

@HiltViewModel
class CreateGameViewModel @Inject constructor(
    private val sessionController: SessionController,
    private val gameSession: GameSession,
    private val settingsRepository: SettingsRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {
    private val _state = MutableStateFlow<GameCreationState>(GameCreationState.InProgress)
    val state = _state.asStateFlow()

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        createGame()
    }

    private fun createGame() {
        viewModelScope.launch(ioDispatcher) {
            //TODO: Important: replace it with GameCodeGenerator.generate() when done testing
            val gameCode = "AAAA"
            val settings = settingsRepository.userSettings.first()
            sessionController.startHost(gameCode, settings.playerId)

            try {
                withTimeout(30_000L) {
                    gameSession.sessionState.first { it is SessionState.Running }
                    gameSession.activeClient!!.registerPlayer(
                        settings.playerName!!,
                        settings.playerId
                    )
                    _state.value = GameCreationState.Success
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "CreateGameViewModel: Couldn't create game (Timeout)")
                _state.value = GameCreationState.Error
            } catch (e: NullPointerException) {
                Log.e(TAG, "CreateGameViewModel: Couldn't create game (Client not initialized)")
                _state.value = GameCreationState.Error
            }
        }
    }

    fun onEvent(event: CreateGameEvent) {
        when (event) {
            CreateGameEvent.GameCreated -> {
                viewModelScope.launch {
                    _eventFlow.emit(UiEvent.ShowSnackBar(R.string.game_found_joining))
                }
            }

            CreateGameEvent.GameCreationFailed -> {
                viewModelScope.launch {
                    sessionController.stopSession()
                    _eventFlow.emit(UiEvent.ShowSnackBar(R.string.couldn_t_create_game_try_again_later))
                    _eventFlow.emit(UiEvent.NavigateTo(Routes.MainMenu.route))
                }
            }

            CreateGameEvent.CancelGameCreation -> {
                viewModelScope.launch {
                    sessionController.stopSession()
                    _eventFlow.emit(UiEvent.NavigateTo(Routes.MainMenu.route))
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.i(TAG, "CreateGameViewModel: Cleared!")
    }
}