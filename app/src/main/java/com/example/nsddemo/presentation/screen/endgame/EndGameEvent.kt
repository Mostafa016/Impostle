package com.example.nsddemo.presentation.screen.endgame

sealed interface EndGameEvent {
    data object EndGame : EndGameEvent
}
