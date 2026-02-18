package com.example.nsddemo.presentation.screen.lobby

sealed interface LobbyEvent {
    data object ChooseCategoryButtonClick : LobbyEvent
    data object StartRound : LobbyEvent
    data class KickPlayer(val playerId: String) : LobbyEvent
}