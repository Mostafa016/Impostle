package com.example.nsddemo.presentation.screen.voting

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.nsddemo.core.util.Debugging.TAG
import com.example.nsddemo.domain.engine.GameSession
import com.example.nsddemo.presentation.util.BaseGameViewModel
import com.example.nsddemo.presentation.util.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VotingViewModel
    @Inject
    constructor(
        gameSession: GameSession,
    ) : BaseGameViewModel(gameSession) {
        private val _state =
            MutableStateFlow(
                gameData.value.localPlayerVotedTargetPlayer?.let {
                    VotingState(
                        votedPlayer = it,
                        isVoteConfirmed = true,
                    )
                } ?: VotingState(),
            )
        val state = _state.asStateFlow()

        private val _eventFlow = MutableSharedFlow<UiEvent>()
        val eventFlow = _eventFlow.asSharedFlow()

        // Static list of candidates (Others)
        val playersExcludingCurrent = gameData.value.otherPlayers

        fun onEvent(event: VotingEvent) {
            when (event) {
                is VotingEvent.VoteForPlayer -> {
                    _state.value = _state.value.copy(votedPlayer = event.player)
                }

                VotingEvent.VoteConfirmed -> {
                    val target = _state.value.votedPlayer ?: return
                    _state.value = _state.value.copy(isVoteConfirmed = true)
                viewModelScope.launch {
                    activeClient?.submitVote(target.id)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.i(TAG, "VotingViewModel: Cleared!")
    }
}
