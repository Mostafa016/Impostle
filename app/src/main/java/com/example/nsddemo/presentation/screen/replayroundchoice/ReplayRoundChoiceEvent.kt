package com.example.nsddemo.presentation.screen.replayroundchoice

sealed interface ReplayRoundChoiceEvent {
    object StartVote : ReplayRoundChoiceEvent

    object ReplayRound : ReplayRoundChoiceEvent
}
