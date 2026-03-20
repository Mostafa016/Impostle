package com.mostafa.impostle.presentation.screen.pause

import com.mostafa.impostle.domain.model.Player

data class PauseState(
    val disconnectedPlayers: List<Player> = emptyList(),
    val isEndGameButtonEnabled: Boolean = true,
)
