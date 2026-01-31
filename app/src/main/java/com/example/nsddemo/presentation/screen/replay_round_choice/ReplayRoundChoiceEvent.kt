package com.example.nsddemo.presentation.screen.replay_round_choice

sealed interface ReplayRoundChoiceEvent {
    object StartVote : ReplayRoundChoiceEvent
    object ReplayRound : ReplayRoundChoiceEvent
}