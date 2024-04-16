package com.example.nsddemo

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class GameRepository {
    private val _gameData = MutableStateFlow(GameData())
    val gameData = _gameData.asStateFlow()

    private val _gameState = MutableStateFlow<GameState>(GameState.StartGame)
    val gameState = _gameState.asStateFlow()

    private val _clientGameState = MutableStateFlow<GameState.ClientGameState?>(null)
    val clientGameState = _clientGameState.asStateFlow()

    fun updateGameData(gameData: GameData) {
        _gameData.value = gameData
    }

    fun updateGameState(gameState: GameState) {
        _gameState.value = gameState
    }

    fun updateClientGameState(clientGameState: GameState.ClientGameState?) {
        _clientGameState.value = clientGameState
    }
}