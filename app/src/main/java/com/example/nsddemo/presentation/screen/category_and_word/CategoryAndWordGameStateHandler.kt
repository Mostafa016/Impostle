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
                is GameState.DisplayCategoryAndWord -> {
//                    serverGameStateManager.handleDisplayCategoryAndWordState()
                }
                is GameState.ConfirmCurrentPlayerReadCategoryAndWord -> serverGameStateManager.handleConfirmCurrentPlayerCategoryAndWord()
                is GameState.GetPlayerReadCategoryAndWordConfirmation -> serverGameStateManager.handleGetPlayerReadCategoryAndWordConfirmationState()
                is GameState.AskQuestion -> {
                    // Do nothing
                    // This is to fix asking the first question when still on this screen
                    // WITHOUT causing duplicate first question handling
                    // So basically, handleGetPlayerReadCategoryAndWordConfirmationState()
                    // updates the state to AskQuestion but we won't react to it here
                    // we will in AskQuestionScreen
                }
                else -> {
                    throw InvalidStateException(it, gameRepository.screenAllowedStates.value)
                }
            }
        }
    }
}