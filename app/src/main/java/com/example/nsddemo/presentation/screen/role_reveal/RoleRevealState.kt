package com.example.nsddemo.presentation.screen.role_reveal

import com.example.nsddemo.domain.model.Player

data class RoleRevealState(
    val isConfirmPressed: Boolean = false,
    val playersWithReadyState: List<PlayerWithReadyState> = emptyList()
)

data class PlayerWithReadyState(
    val name: String,
    val color: String,
    val id: String = "",
    val isConnected: Boolean = true,
    val isReady: Boolean = false,
)

fun Player.withReadyState(readyState: Boolean) = PlayerWithReadyState(
    id = id, name = name,
    color = color,
    isConnected = isConnected,
    isReady = readyState,
)

