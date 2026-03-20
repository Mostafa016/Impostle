package com.mostafa.impostle.presentation.screen.replayroundchoice

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.mostafa.impostle.core.util.Debugging.TAG
import com.mostafa.impostle.domain.engine.GameSession
import com.mostafa.impostle.presentation.util.BaseGameViewModel
import com.mostafa.impostle.presentation.util.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReplayRoundChoiceViewModel
    @Inject
    constructor(
        gameSession: GameSession,
    ) : BaseGameViewModel(gameSession) {
        private val _state = MutableStateFlow(ReplayRoundChoiceState())
        val state = _state.asStateFlow()

        private val _eventFlow = MutableSharedFlow<UiEvent>()
        val eventFlow = _eventFlow.asSharedFlow()

        // --- Static Property (Synchronous) ---
        val isHost: Boolean = gameData.value.isHost

        fun onEvent(event: ReplayRoundChoiceEvent) {
            when (event) {
                ReplayRoundChoiceEvent.ReplayRound -> {
                    _state.value = state.value.copy(isReplayRoundButtonEnabled = false)
                    viewModelScope.launch { activeClient?.replayRound() }
                }

                ReplayRoundChoiceEvent.StartVote -> {
                    _state.value = state.value.copy(isStartVoteButtonEnabled = false)
                    viewModelScope.launch { activeClient?.startVote() }
                }
            }
        }

        override fun onCleared() {
            super.onCleared()
            Log.i(TAG, "RoundReplayViewModel: Cleared!")
        }
    }
