package com.example.nsddemo.presentation.screen.choose_extra_questions

import com.example.nsddemo.core.util.GameState
import com.example.nsddemo.data.repository.GameRepository
import com.example.nsddemo.data.util.InvalidStateException
import com.example.nsddemo.domain.use_case.ServerGameStateManager
import com.example.nsddemo.presentation.util.BaseGameStateHandler

class ChooseExtraQuestionsGameStateHandler(
    gameRepository: GameRepository, serverGameStateManager: ServerGameStateManager
) : BaseGameStateHandler(gameRepository, serverGameStateManager) {
    override suspend fun handleGameStateChanges() {
        gameRepository.gameState.collect {
            when (it) {
                GameState.StartVote -> {
                    serverGameStateManager.handleStartVoteState()
                    _lastStateHandlerListener.value = true
                }

                GameState.AskExtraQuestions -> {
                    serverGameStateManager.handleAskExtraQuestionsState()
                    _lastStateHandlerListener.value = true
                }

                else -> {
                    throw InvalidStateException(it, gameRepository.screenAllowedStates.value)
                }
            }
        }
    }
}