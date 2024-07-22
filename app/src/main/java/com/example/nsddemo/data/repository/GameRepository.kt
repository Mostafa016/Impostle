package com.example.nsddemo.data.repository

import android.content.SharedPreferences
import com.example.nsddemo.core.util.GameState
import com.example.nsddemo.data.util.GameStateValidator
import com.example.nsddemo.domain.model.GameData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class GameRepository(private val sharedPreferences: SharedPreferences) {
    private val _gameData = MutableStateFlow(GameData())
    val gameData = _gameData.asStateFlow()

    private val _gameState = MutableStateFlow<GameState>(GameState.StartGame)
    val gameState = _gameState.asStateFlow()

    private val _screenAllowedStates = MutableStateFlow<Set<String>>(emptySet())
    val screenAllowedStates = _screenAllowedStates.asStateFlow()

    private val _clientGameState = MutableStateFlow<GameState.ClientGameState?>(null)
    val clientGameState = _clientGameState.asStateFlow()

    var playerName: String? = null
        get() {
            if (field != null) {
                return field
            }
            field = sharedPreferences.getString(PLAYER_NAME_KEY, null)
            return field
        }
        set(updatedPlayerName) {
            field = updatedPlayerName!!
            val currentPlayer = gameData.value.currentPlayer?.copy(name = updatedPlayerName)
            _gameData.value = gameData.value.copy(currentPlayer = currentPlayer)
            sharedPreferences.edit().putString(PLAYER_NAME_KEY, field).apply()
        }

    init {
        playerName?.let {
            val currentPlayer = gameData.value.currentPlayer?.copy(name = it)
            _gameData.value = gameData.value.copy(currentPlayer = currentPlayer)
        }
    }

    fun updateGameData(gameData: GameData) {
        _gameData.value = gameData
    }

    fun updateGameState(newGameState: GameState) {
        val currentGameState = _gameState.value.let {
            if (it is GameState.Transitioning) it.from
            else it
        }
        GameStateValidator.validateState(currentGameState, newGameState, screenAllowedStates.value)
        _gameState.value = newGameState
    }

    fun updateClientGameState(clientGameState: GameState.ClientGameState?) {
        _clientGameState.value = clientGameState
    }

    fun setAllowedStates(states: Set<String>) {
        _screenAllowedStates.value = states
    }

    private companion object {
        const val PLAYER_NAME_KEY = "impostle_player_name"
    }
}