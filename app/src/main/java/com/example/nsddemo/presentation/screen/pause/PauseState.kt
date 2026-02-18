package com.example.nsddemo.presentation.screen.pause

import com.example.nsddemo.domain.model.Player

data class PauseState(
    val disconnectedPlayers: List<Player> = emptyList(),
    val isEndGameButtonEnabled: Boolean = true,
)
