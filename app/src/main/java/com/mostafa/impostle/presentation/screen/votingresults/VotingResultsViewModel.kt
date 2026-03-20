package com.mostafa.impostle.presentation.screen.votingresults

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.mostafa.impostle.core.util.Debugging.TAG
import com.mostafa.impostle.domain.engine.GameSession
import com.mostafa.impostle.domain.model.Player
import com.mostafa.impostle.presentation.util.BaseGameViewModel
import com.mostafa.impostle.presentation.util.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VotingResultsViewModel
    @Inject
    constructor(
        gameSession: GameSession,
    ) : BaseGameViewModel(gameSession) {
        private val _eventFlow = MutableSharedFlow<UiEvent>()
        val eventFlow = _eventFlow.asSharedFlow()

        // Static Properties
        val isHost: Boolean = gameData.value.isHost
        val currentPlayer: Player = gameData.value.localPlayer!!
        val isImposter: Boolean = gameData.value.isImposter
        val imposter: Player = gameData.value.players[gameData.value.imposterId]!!
        val roundVotingCounts = gameData.value.voteCountsAsPlayers

        fun onEvent(event: VotingResultsEvent) {
            when (event) {
                VotingResultsEvent.ShowScores -> {
                    viewModelScope.launch {
                        activeClient?.continueToGameChoice()
                    }
                }
            }
        }

        override fun onCleared() {
            super.onCleared()
            Log.i(TAG, "VotingResultsViewModel: Cleared!")
        }
    }
