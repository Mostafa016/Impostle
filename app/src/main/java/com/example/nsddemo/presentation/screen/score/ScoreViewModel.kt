package com.example.nsddemo.presentation.screen.score

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.nsddemo.R
import com.example.nsddemo.core.util.Debugging.TAG
import com.example.nsddemo.domain.engine.GameSession
import com.example.nsddemo.domain.model.GamePhase
import com.example.nsddemo.domain.model.Player
import com.example.nsddemo.presentation.util.BaseGameViewModel
import com.example.nsddemo.presentation.util.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScoreViewModel
    @Inject
    constructor(
        gameSession: GameSession,
    ) : BaseGameViewModel(gameSession) {
        private val _state = MutableStateFlow(ScoreState())
        val state = _state.asStateFlow()

        private val _eventFlow = MutableSharedFlow<UiEvent>()
        val eventFlow = _eventFlow.asSharedFlow()

        // Static Properties
        val isHost: Boolean = gameData.value.isHost
        val currentPlayer: Player = gameData.value.localPlayer ?: Player("", "")
        val isImposter: Boolean = gameData.value.isImposter
        val imposter: Player =
            gameData.value.players[gameData.value.imposterId] ?: Player("Unknown", "FF000000")
        val playerScores = gameData.value.scoresAsPlayers

        init {
            viewModelScope.launch(Dispatchers.IO) {
                gamePhase.collectLatest { phase ->
                    if (phase is GamePhase.Lobby) {
                        _eventFlow.emit(UiEvent.ShowSnackBar(R.string.continuing_playing))
                    }
                }
            }
        }

        fun onEvent(event: ScoreEvent) {
            when (event) {
                ScoreEvent.ReplayGame -> {
                    viewModelScope.launch(Dispatchers.IO) { activeClient?.replayGame() }
                }

                ScoreEvent.EndGame -> {
                    viewModelScope.launch(Dispatchers.IO) {
                        activeClient?.endGame()
                    }
                }
            }
        }

        override fun onCleared() {
            super.onCleared()
            Log.i(TAG, "ScoreViewModel: Cleared!")
        }
    }
