package com.mostafa.impostle.presentation.screen.joingame

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mostafa.impostle.R
import com.mostafa.impostle.core.util.Debugging.TAG
import com.mostafa.impostle.core.util.GameConstants
import com.mostafa.impostle.di.IoDispatcher
import com.mostafa.impostle.domain.engine.GameSession
import com.mostafa.impostle.domain.model.SessionState
import com.mostafa.impostle.domain.repository.SettingsRepository
import com.mostafa.impostle.presentation.service.SessionController
import com.mostafa.impostle.presentation.util.Routes
import com.mostafa.impostle.presentation.util.UiEvent
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
class JoinGameViewModel
    @Inject
    constructor(
        private val sessionController: SessionController,
        private val gameSession: GameSession,
        private val settingsRepository: SettingsRepository,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
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
            _state.value =
                state.value.copy(
                    gameCodeTextFieldText = text.uppercase(),
                    isJoinGameButtonEnabled = text.length == GameConstants.CODE_LENGTH,
                )
        }

        private fun onJoinGamePressed() {
            _state.value =
                state.value.copy(isJoinGameButtonEnabled = false, gameCodeTextFieldEnabled = false)
            val gameCode = state.value.gameCodeTextFieldText
            viewModelScope.launch(ioDispatcher) {
                _eventFlow.emit(UiEvent.NavigateTo(Routes.JoinGameLoading.route))

                val settings = settingsRepository.userSettings.first()
                sessionController.startJoin(gameCode, settings.playerId)

                try {
                    withTimeout(15_000L) {
                        val sessionState =
                            gameSession.sessionState.first { it is SessionState.Running || it is SessionState.Error }
                        if (sessionState is SessionState.Error) {
                            throw IllegalStateException(sessionState.reason)
                        }
                        gameSession.activeClient!!.registerPlayer(
                            settings.playerName!!,
                            settings.playerId,
                        )
                        _eventFlow.emit(UiEvent.ShowSnackBar(R.string.game_found_joining))
                    }
                } catch (e: TimeoutCancellationException) {
                    Log.e(TAG, "JoinGameViewModel: Couldn't join game (Timeout)")
                    onJoinFailed()
                } catch (e: NullPointerException) {
                    Log.e(TAG, "JoinGameViewModel: Couldn't join game (Client not initialized)")
                    onJoinFailed()
                } catch (e: IllegalStateException) {
                    Log.e(TAG, "JoinGameViewModel: Couldn't join game (Initialization failed)")
                    onJoinFailed()
                }
            }
        }

        override fun onCleared() {
            super.onCleared()
            Log.i(TAG, "JoinGameViewModel: Cleared!")
        }

        private fun onGoBackToMainMenu() {
            sessionController.stopSession()
            viewModelScope.launch {
                _eventFlow.emit(UiEvent.NavigateTo(Routes.MainMenu.route))
            }
        }

        private suspend fun onJoinFailed() {
            sessionController.stopSession()
            _state.value = JoinGameState()
            _eventFlow.emit(UiEvent.ShowSnackBar(R.string.game_not_found))
            _eventFlow.emit(UiEvent.NavigateTo(Routes.JoinGame.route))
        }
    }
