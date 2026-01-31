package com.example.nsddemo.presentation.screen.voting_results

import androidx.lifecycle.viewModelScope
import com.example.nsddemo.domain.engine.GameSession
import com.example.nsddemo.domain.model.GamePhase
import com.example.nsddemo.domain.model.Player
import com.example.nsddemo.presentation.util.BaseGameViewModel
import com.example.nsddemo.presentation.util.Routes
import com.example.nsddemo.presentation.util.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VotingResultsViewModel @Inject constructor(
    gameSession: GameSession
) : BaseGameViewModel(gameSession) {

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    // Static Properties
    val isHost: Boolean = gameData.value.isHost
    val currentPlayer: Player = gameData.value.localPlayer!!
    val isImposter: Boolean = gameData.value.isImposter
    val imposter: Player = gameData.value.players[gameData.value.imposterId]!!
    val roundVotingCounts = gameData.value.voteCountsAsPlayers

    init {
        viewModelScope.launch {
            gamePhase.collectLatest { phase ->
                if (phase is GamePhase.GameReplayChoice) {
                    _eventFlow.emit(UiEvent.NavigateTo(Routes.Scoreboard.route))
                }
            }
        }
    }

    fun onEvent(event: VotingResultsEvent) {
        when (event) {
            VotingResultsEvent.ShowScores -> {
                viewModelScope.launch {
                    activeClient?.continueToGameChoice()
                }
            }

            else -> {}
        }
    }
}