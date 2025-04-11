package com.example.nsddemo.presentation.screen.score

import com.example.nsddemo.core.util.GameState
import com.example.nsddemo.data.repository.GameRepository
import com.example.nsddemo.data.util.InvalidStateException
import com.example.nsddemo.domain.use_case.ServerGameStateManager
import com.example.nsddemo.presentation.util.BaseGameStateHandler

class ScoreGameStateHandler(
    gameRepository: GameRepository, serverGameStateManager: ServerGameStateManager
) : BaseGameStateHandler(gameRepository, serverGameStateManager) {
    override suspend fun handleGameStateChanges() {
        gameRepository.gameState.collect {
            when (it) {
                is GameState.Replay -> {
                    serverGameStateManager.handleReplayState()
                    _lastStateHandlerListener.value = true
                }

                is GameState.Transitioning -> {
                    // Do nothing
                }

                is GameState.ShowScoreboard -> {
                    // Do nothing
                    // Same concept as CategoryAndWordStateHandler
                }

                is GameState.StartGame -> {
                    // Do nothing
                    // Same concept as CategoryAndWordStateHandler
                }

                is GameState.StartNewRound -> {
                    // Do nothing
                    // Same concept as CategoryAndWordStateHandler
                }

                else -> {
                    throw InvalidStateException(it, gameRepository.screenAllowedStates.value)
                }
            }
        }
    }
}