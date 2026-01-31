package com.example.nsddemo.presentation.screen.end_game

sealed interface EndGameEvent {
    data object EndGame : EndGameEvent
}