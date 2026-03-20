package com.mostafa.impostle.presentation.screen.voting

import com.mostafa.impostle.domain.model.Player

sealed interface VotingEvent {
    data class VoteForPlayer(
        val player: Player,
    ) : VotingEvent

    data object VoteConfirmed : VotingEvent
}
