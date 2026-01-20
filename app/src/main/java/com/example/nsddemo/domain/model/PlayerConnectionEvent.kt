package com.example.nsddemo.domain.model

sealed interface PlayerConnectionEvent {
    data class PlayerConnected(val id: String, val playerName: String) : PlayerConnectionEvent
    data class PlayerDisconnected(val id: String) : PlayerConnectionEvent
}