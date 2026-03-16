package com.example.nsddemo.presentation.screen.voting

import com.example.nsddemo.domain.model.Player

sealed interface VotingEvent {
    data class VoteForPlayer(
        val player: Player,
    ) : VotingEvent

    data object VoteConfirmed : VotingEvent
}
