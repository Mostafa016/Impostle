package com.mostafa.impostle.presentation.screen.replayroundchoice

sealed interface ReplayRoundChoiceEvent {
    object StartVote : ReplayRoundChoiceEvent

    object ReplayRound : ReplayRoundChoiceEvent
}
