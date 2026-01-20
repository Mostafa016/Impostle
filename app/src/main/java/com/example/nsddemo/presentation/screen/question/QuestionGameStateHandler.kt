package com.example.nsddemo.presentation.screen.question

import com.example.nsddemo.core.util.GameState
import com.example.nsddemo.data.repository.GameRepository
import com.example.nsddemo.data.util.InvalidStateException
import com.example.nsddemo.domain.legacy.ServerGameStateManager
import com.example.nsddemo.presentation.util.BaseGameStateHandler

class QuestionGameStateHandler(
    gameRepository: GameRepository, serverGameStateManager: ServerGameStateManager
) : BaseGameStateHandler(gameRepository, serverGameStateManager) {
    override suspend fun handleGameStateChanges() {
        gameRepository.gameState.collect {
            when (it) {
                is GameState.AskQuestion -> serverGameStateManager.handleAskQuestionState()
                is GameState.ConfirmCurrentPlayerQuestion -> serverGameStateManager.handleConfirmCurrentPlayerQuestion()
                is GameState.ChooseExtraQuestions -> {
                    serverGameStateManager.handleChooseExtraQuestionsState()
                    _lastStateHandlerListener.value = true
                }

                else -> {
                    throw InvalidStateException(it, gameRepository.screenAllowedStates.value)
                }
            }
        }
    }
}