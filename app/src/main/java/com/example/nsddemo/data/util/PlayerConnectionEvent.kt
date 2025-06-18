package com.example.nsddemo.data.util

import com.example.nsddemo.domain.model.Player

sealed interface PlayerConnectionEvent {
    data class PlayerConnected(val id: String, val playerName: String) : PlayerConnectionEvent
    data class PlayerDisconnected(val id: String, val player: Player) : PlayerConnectionEvent
}