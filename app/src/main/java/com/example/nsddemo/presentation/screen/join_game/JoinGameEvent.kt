package com.example.nsddemo.presentation.screen.join_game

sealed interface JoinGameEvent {
    data class GameCodeTextFieldValueChange(val text: String) : JoinGameEvent
    object JoinGame : JoinGameEvent
    object GoBackToMainMenu : JoinGameEvent

    object GameFound : JoinGameEvent
    object GameStarted : JoinGameEvent
    object GameSearchTimedOut : JoinGameEvent
}