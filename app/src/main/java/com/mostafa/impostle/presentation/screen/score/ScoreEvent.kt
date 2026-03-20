package com.mostafa.impostle.presentation.screen.score

sealed interface ScoreEvent {
    data object ReplayGame : ScoreEvent

    data object EndGame : ScoreEvent
}
