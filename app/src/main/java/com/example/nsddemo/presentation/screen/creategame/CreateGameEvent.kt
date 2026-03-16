package com.example.nsddemo.presentation.screen.creategame

sealed interface CreateGameEvent {
    data object GameCreated : CreateGameEvent

    data object GameCreationFailed : CreateGameEvent

    data object CancelGameCreation : CreateGameEvent
}
