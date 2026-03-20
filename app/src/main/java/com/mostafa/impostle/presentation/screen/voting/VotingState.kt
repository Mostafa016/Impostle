package com.mostafa.impostle.presentation.screen.voting

import com.mostafa.impostle.domain.model.Player

data class VotingState(
    val votedPlayer: Player? = null,
    val isVoteConfirmed: Boolean = false,
) {
    val hasChosenVote: Boolean
        get() = votedPlayer != null
}
