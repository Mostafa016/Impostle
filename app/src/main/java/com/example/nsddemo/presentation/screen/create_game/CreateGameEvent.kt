package com.example.nsddemo.presentation.screen.create_game

sealed interface CreateGameEvent {
    object GameCreated : CreateGameEvent
}