package com.example.nsddemo.presentation.screen.voting_results

import com.example.nsddemo.core.util.GameState
import com.example.nsddemo.data.repository.GameRepository
import com.example.nsddemo.data.util.InvalidStateException
import com.example.nsddemo.domain.use_case.ServerGameStateManager
import com.example.nsddemo.presentation.util.BaseGameStateHandler

class VotingResultsGameStateHandler(
    gameRepository: GameRepository, serverGameStateManager: ServerGameStateManager
) : BaseGameStateHandler(gameRepository, serverGameStateManager) {
    override suspend fun handleGameStateChanges() {
        when (val currentGameState = gameRepository.gameState.value) {
            GameState.ShowScoreboard -> serverGameStateManager.handleShowScoreboardState()
            is GameState.Transitioning -> {
                // Do nothing
            }

            is GameState.EndVote -> {
                // Do nothing
                // Same as CategoryAndWordStateHandler
            }

            is GameState.Replay -> serverGameStateManager.handleReplayState()

            is GameState.StartGame -> {
                // Do nothing
            }

            else -> {
                throw InvalidStateException(
                    currentGameState,
                    gameRepository.screenAllowedStates.value
                )
            }
        }
    }
}