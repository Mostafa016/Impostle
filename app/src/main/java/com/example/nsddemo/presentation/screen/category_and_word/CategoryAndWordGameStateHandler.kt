package com.example.nsddemo.presentation.screen.category_and_word

import com.example.nsddemo.core.util.GameState
import com.example.nsddemo.data.repository.GameRepository
import com.example.nsddemo.data.util.InvalidStateException
import com.example.nsddemo.domain.use_case.ServerGameStateManager
import com.example.nsddemo.presentation.util.BaseGameStateHandler

class CategoryAndWordGameStateHandler(
    gameRepository: GameRepository, serverGameStateManager: ServerGameStateManager
) : BaseGameStateHandler(gameRepository, serverGameStateManager) {
    override suspend fun handleGameStateChanges() {
        gameRepository.gameState.collect {
            when (it) {
                is GameState.ConfirmCurrentPlayerReadCategoryAndWord -> serverGameStateManager.handleConfirmCurrentPlayerCategoryAndWord()
                is GameState.GetPlayerReadCategoryAndWordConfirmation -> serverGameStateManager.handleGetPlayerReadCategoryAndWordConfirmationState()
                else -> {
                    throw InvalidStateException(it, gameRepository.screenAllowedStates.value)
                }
            }
        }
    }
}