package com.mostafa.impostle.presentation.screen.pause

sealed interface PauseEvent {
    data class KickPlayer(
        val playerId: String,
    ) : PauseEvent

    data object EndGame : PauseEvent

    data object ContinueGameAnyway : PauseEvent
}
