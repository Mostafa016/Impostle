package com.example.nsddemo.presentation.screen.score

sealed interface ScoreEvent {
    object ReplayGame : ScoreEvent
    object ReplayGameServerSide : ScoreEvent
    object EndGame : ScoreEvent

    object EndGameServerSide : ScoreEvent
}