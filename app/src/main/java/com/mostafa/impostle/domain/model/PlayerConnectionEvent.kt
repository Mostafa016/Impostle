package com.mostafa.impostle.domain.model

sealed interface PlayerConnectionEvent {
    data class PlayerConnected(
        val id: String,
        val playerName: String,
    ) : PlayerConnectionEvent

    data class PlayerDisconnected(
        val id: String,
    ) : PlayerConnectionEvent
}
