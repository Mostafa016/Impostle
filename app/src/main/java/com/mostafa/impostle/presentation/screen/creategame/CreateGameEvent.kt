package com.mostafa.impostle.presentation.screen.creategame

sealed interface CreateGameEvent {
    data object GameCreated : CreateGameEvent

    data object GameCreationFailed : CreateGameEvent

    data object CancelGameCreation : CreateGameEvent
}
