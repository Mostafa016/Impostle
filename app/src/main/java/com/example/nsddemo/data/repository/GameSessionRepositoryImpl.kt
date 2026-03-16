package com.example.nsddemo.data.repository

import com.example.nsddemo.domain.model.GameData
import com.example.nsddemo.domain.model.GamePhase
import com.example.nsddemo.domain.repository.GameSessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class GameSessionRepositoryImpl
    @Inject
    constructor() : GameSessionRepository {
        private val _gameData: MutableStateFlow<GameData> = MutableStateFlow(GameData())
        override val gameData = _gameData.asStateFlow()

        private val _gameState: MutableStateFlow<GamePhase> = MutableStateFlow(GamePhase.Idle)
        override val gameState = _gameState.asStateFlow()

        override fun updateGamePhase(newState: GamePhase) =
            _gameState.update { currentState ->
                currentState.checkTransitionOrThrow(newState)
                newState
            }

        override fun updateGameData(transform: (GameData) -> GameData) = _gameData.update(transform)

        override fun reset() {
            _gameState.value = GamePhase.Idle
            _gameData.value = GameData()
        }
    }
