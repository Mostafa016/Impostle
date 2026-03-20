package com.mostafa.impostle.presentation.screen.pause

import androidx.lifecycle.viewModelScope
import com.mostafa.impostle.di.IoDispatcher
import com.mostafa.impostle.domain.engine.GameSession
import com.mostafa.impostle.presentation.util.BaseGameViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PauseViewModel
    @Inject
    constructor(
        gameSession: GameSession,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : BaseGameViewModel(gameSession) {
        private val isEndGameButtonPressed = MutableStateFlow(false)
        val state: StateFlow<PauseState> =
            combine(
                gameData,
                isEndGameButtonPressed,
            ) { data, isEndGameButtonPressed ->
                PauseState(
                    isEndGameButtonEnabled = !isEndGameButtonPressed,
                    disconnectedPlayers = data.players.values.filter { !it.isConnected },
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = PauseState(),
            )

        val isHost = gameData.value.isHost
        val gameCode = gameData.value.gameCode
        val localPlayerId = gameData.value.localPlayerId

        fun onEvent(event: PauseEvent) {
            when (event) {
                is PauseEvent.KickPlayer -> kickPlayer(event.playerId)
                PauseEvent.EndGame -> endGame()
                PauseEvent.ContinueGameAnyway -> continueAnyway()
            }
        }

        private fun continueAnyway() {
            for (disconnectedPlayer in state.value.disconnectedPlayers) {
                viewModelScope.launch(ioDispatcher) {
                    activeClient?.kickPlayer(disconnectedPlayer.id)
                }
            }
        }

        private fun kickPlayer(playerId: String) {
            viewModelScope.launch(ioDispatcher) {
                activeClient?.kickPlayer(playerId)
            }
        }

        private fun endGame() {
            val isEndGameButtonPressedState = isEndGameButtonPressed.value
            if (isEndGameButtonPressedState) {
                return
            }
            isEndGameButtonPressed.value = true
            viewModelScope.launch(ioDispatcher) {
                activeClient?.endGame()
            }
        }
    }
