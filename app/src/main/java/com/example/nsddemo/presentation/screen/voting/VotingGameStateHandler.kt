package com.example.nsddemo.presentation.screen.voting

import com.example.nsddemo.core.util.GameState
import com.example.nsddemo.data.repository.GameRepository
import com.example.nsddemo.data.util.InvalidStateException
import com.example.nsddemo.domain.legacy.ServerGameStateManager
import com.example.nsddemo.presentation.util.BaseGameStateHandler

class VotingGameStateHandler(
    gameRepository: GameRepository, serverGameStateManager: ServerGameStateManager
) : BaseGameStateHandler(gameRepository, serverGameStateManager) {
    override suspend fun handleGameStateChanges() {
        gameRepository.gameState.collect {
            when (it) {
                is GameState.StartVote -> {
                    // Do nothing
                    // Same concept as CategoryAndWordStateHandler
                }

                is GameState.GetCurrentPlayerVote -> serverGameStateManager.handleGetCurrentPlayerVoteState()

                is GameState.GetPlayerVote -> serverGameStateManager.handleGetPlayerVoteState()

                is GameState.EndVote -> {
                    serverGameStateManager.handleEndVoteState()
                    _lastStateHandlerListener.value = true
                }

                else -> {
                    throw InvalidStateException(it, gameRepository.screenAllowedStates.value)
                }
            }
        }
    }
}