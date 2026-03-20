package com.mostafa.impostle.domain.repository

import com.mostafa.impostle.domain.model.GameData
import com.mostafa.impostle.domain.model.GamePhase
import kotlinx.coroutines.flow.StateFlow

interface GameSessionRepository {
    val gameData: StateFlow<GameData>
    val gameState: StateFlow<GamePhase>

    fun updateGamePhase(newState: GamePhase)

    fun updateGameData(transform: (GameData) -> GameData)

    fun reset()
}
