package com.example.nsddemo.presentation.screen.join_game

import com.example.nsddemo.data.repository.GameRepository
import com.example.nsddemo.domain.legacy.ServerGameStateManager
import com.example.nsddemo.presentation.util.BaseGameStateHandler

class JoinGameStateHandler(
    gameRepository: GameRepository,
    serverGameStateManager: ServerGameStateManager
) : BaseGameStateHandler(gameRepository, serverGameStateManager) {
    override suspend fun handleGameStateChanges() {
        when (gameRepository.gameState.value) {
            else -> {
                // Do nothing
            }
        }
    }
}