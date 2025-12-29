package com.example.nsddemo.data.repository

import com.example.nsddemo.domain.model.GamePhase
import com.example.nsddemo.domain.model.Idle
import com.example.nsddemo.domain.model.NewGameData
import com.example.nsddemo.domain.repository.GameSessionRepository
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class GameSessionRepositoryImpl @Inject constructor() : GameSessionRepository {
    private val _gameData: MutableStateFlow<NewGameData> = MutableStateFlow(NewGameData())
    override val gameData = _gameData.asStateFlow()

    private val _gameState: MutableStateFlow<GamePhase> = MutableStateFlow(Idle)
    override val gameState = _gameState.asStateFlow()

    override fun updateGameState(newState: GamePhase) =
        _gameState.update { currentState ->
            currentState.checkTransitionOrThrow(newState)
            newState
        }

    override fun updateGameData(transform: (NewGameData) -> NewGameData) =
        _gameData.update(transform)

    override fun reset() {
        _gameState.value = Idle
        _gameData.value = NewGameData()
    }
}