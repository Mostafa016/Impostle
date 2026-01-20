package com.example.nsddemo.domain.repository

import com.example.nsddemo.domain.model.GamePhase
import com.example.nsddemo.domain.model.NewGameData
import kotlinx.coroutines.flow.StateFlow


interface GameSessionRepository {
    val gameData: StateFlow<NewGameData>
    val gameState: StateFlow<GamePhase>

    fun updateGamePhase(newState: GamePhase)
    fun updateGameData(transform: (NewGameData) -> NewGameData)

    fun reset()
}