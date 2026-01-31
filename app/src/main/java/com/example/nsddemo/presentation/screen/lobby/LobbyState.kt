package com.example.nsddemo.presentation.screen.lobby

import com.example.nsddemo.domain.model.GameCategory
import com.example.nsddemo.domain.model.Player

data class LobbyState(
    val chosenCategory: GameCategory? = null,
    val players: List<Player> = emptyList(),
    val isStartRoundButtonEnabled: Boolean = false,
)
