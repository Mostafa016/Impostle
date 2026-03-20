package com.mostafa.impostle.presentation.screen.joingame

sealed interface JoinGameEvent {
    data class GameCodeTextFieldValueChange(
        val text: String,
    ) : JoinGameEvent

    data object JoinGame : JoinGameEvent

    data object GoBackToMainMenu : JoinGameEvent
}
