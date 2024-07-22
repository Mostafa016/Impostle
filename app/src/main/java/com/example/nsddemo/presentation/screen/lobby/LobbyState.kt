package com.example.nsddemo.presentation.screen.lobby

import com.example.nsddemo.domain.model.Player
import com.example.nsddemo.domain.util.Categories

data class LobbyState(
    val chosenCategory: Categories? = null,
    val players: List<Player> = emptyList(),
    val isStartRoundButtonEnabled: Boolean = false,
)
