package com.example.nsddemo.presentation.screen.create_game

sealed interface GameCreationState {
    data object InProgress : GameCreationState
    data object Success : GameCreationState
    data object Error : GameCreationState
}
