package com.mostafa.impostle.domain.strategy

import com.mostafa.impostle.domain.model.ClientMessage
import com.mostafa.impostle.domain.model.GameData
import com.mostafa.impostle.domain.model.GamePhase
import com.mostafa.impostle.domain.model.GameStateTransition
import com.mostafa.impostle.domain.model.RoundData

interface GameModeStrategy {
    val roundData: RoundData

    fun handleAction(
        data: GameData,
        phase: GamePhase,
        message: ClientMessage,
        playerID: String,
    ): GameStateTransition

    fun onPlayerRemoved(
        data: GameData,
        phase: GamePhase,
        removedPlayerId: String,
    ): GameStateTransition
}
