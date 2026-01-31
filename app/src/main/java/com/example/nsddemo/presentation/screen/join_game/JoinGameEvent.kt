package com.example.nsddemo.presentation.screen.join_game

sealed interface JoinGameEvent {
    data class GameCodeTextFieldValueChange(val text: String) : JoinGameEvent
    data object JoinGame : JoinGameEvent
    data object GoBackToMainMenu : JoinGameEvent
}