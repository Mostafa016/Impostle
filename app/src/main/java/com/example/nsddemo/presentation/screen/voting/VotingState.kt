package com.example.nsddemo.presentation.screen.voting

import com.example.nsddemo.domain.model.Player

data class VotingState(
    val votedPlayer: Player? = null,
    val isVoteConfirmed: Boolean = false,
) {
    val hasChosenVote: Boolean
        get() = votedPlayer != null
}
