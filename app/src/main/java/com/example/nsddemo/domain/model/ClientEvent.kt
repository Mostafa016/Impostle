package com.example.nsddemo.domain.model

sealed interface ClientEvent {
    // Errors
    data object LobbyFull : ClientEvent
    data object GameAlreadyStarted : ClientEvent

    // Notifications
    data class PlayerLeft(val playerId: String) : ClientEvent
    data class PlayerRejoined(val playerId: String) : ClientEvent
    data object GameResumed : ClientEvent

    data object KickedFromGame : ClientEvent
    data object LobbyClosed : ClientEvent
}