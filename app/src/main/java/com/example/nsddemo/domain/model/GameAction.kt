package com.example.nsddemo.domain.model

sealed interface GameAction {
    data class User(
        val playerId: String,
        val message: ClientMessage,
    ) : GameAction

    data class System(
        val event: SystemEvent,
    ) : GameAction
}

sealed interface SystemEvent {
    data class PlayerDisconnected(
        val playerId: String,
    ) : SystemEvent
}
