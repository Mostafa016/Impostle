package com.example.nsddemo.presentation.screen.main_menu

import com.example.nsddemo.core.util.GameState
import com.example.nsddemo.data.repository.GameRepository
import com.example.nsddemo.data.util.InvalidStateException
import com.example.nsddemo.domain.use_case.ServerGameStateManager
import com.example.nsddemo.presentation.util.BaseGameStateHandler

class MainMenuGameStateHandler(
    gameRepository: GameRepository,
    serverGameStateManager: ServerGameStateManager
) :
    BaseGameStateHandler(gameRepository, serverGameStateManager) {
    override suspend fun handleGameStateChanges() {
        gameRepository.gameState.collect {
            when (it) {
                is GameState.StartGame -> {
                    serverGameStateManager.handleStartGameState()
                    _lastStateHandlerListener.value = true
                }

                is GameState.Transitioning -> {
                    if (it.to !is GameState.StartGame) {
                        throw InvalidStateException(it.to, gameRepository.screenAllowedStates.value)
                    }
                    gameRepository.updateGameState(it.to)
                }

                else -> {
                    throw InvalidStateException(it, gameRepository.screenAllowedStates.value)
                }
            }
        }
    }
}