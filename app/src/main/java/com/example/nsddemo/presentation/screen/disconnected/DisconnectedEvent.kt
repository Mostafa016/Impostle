package com.example.nsddemo.presentation.screen.disconnected

sealed interface DisconnectedEvent {
    data object GoToMainMenuButtonPressed : DisconnectedEvent
    data object ReconnectButtonPressed : DisconnectedEvent
}