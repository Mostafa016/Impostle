package com.example.nsddemo.presentation.screen.votingresults

sealed interface VotingResultsEvent {
    data object ShowScores : VotingResultsEvent
}
