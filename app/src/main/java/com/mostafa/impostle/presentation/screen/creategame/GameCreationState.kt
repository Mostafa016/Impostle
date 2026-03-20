package com.mostafa.impostle.presentation.screen.creategame

sealed interface GameCreationState {
    data object InProgress : GameCreationState

    data object Success : GameCreationState

    data object Error : GameCreationState
}
