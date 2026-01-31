package com.example.nsddemo.presentation.screen.create_game

sealed interface CreateGameEvent {
    data object GameCreated : CreateGameEvent
    data object GameCreationFailed : CreateGameEvent
}