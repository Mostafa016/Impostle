package com.mostafa.impostle.presentation.screen.lobby

import com.mostafa.impostle.domain.model.GameCategory
import com.mostafa.impostle.domain.model.Player

data class LobbyState(
    val chosenCategory: GameCategory? = null,
    val players: List<Player> = emptyList(),
    val isStartRoundButtonEnabled: Boolean = false,
)
