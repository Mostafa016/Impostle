package com.example.nsddemo.presentation.screen.voting

import com.example.nsddemo.domain.model.Player

sealed interface VotingEvent {
    data class onVotedForPlayer(val player: Player) : VotingEvent
    object onVoteConfirmed : VotingEvent
}