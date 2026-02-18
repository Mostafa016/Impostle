package com.example.nsddemo.domain.repository

import com.example.nsddemo.domain.model.GameData
import com.example.nsddemo.domain.model.GamePhase
import kotlinx.coroutines.flow.StateFlow


interface GameSessionRepository {
    val gameData: StateFlow<GameData>
    val gameState: StateFlow<GamePhase>

    fun updateGamePhase(newState: GamePhase)
    fun updateGameData(transform: (GameData) -> GameData)

    fun reset()
}