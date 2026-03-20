package com.mostafa.impostle.presentation.screen.votingresults

sealed interface VotingResultsEvent {
    data object ShowScores : VotingResultsEvent
}
