package com.mostafa.impostle.presentation.screen.endgame

sealed interface EndGameEvent {
    data object EndGame : EndGameEvent
}
