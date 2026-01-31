package com.example.nsddemo.presentation.screen.voting_results

sealed interface VotingResultsEvent {
    data object ShowScores : VotingResultsEvent
}