package com.example.nsddemo.presentation.screen.lobby

import android.util.Log
import com.example.nsddemo.core.util.Debugging.TAG
import com.example.nsddemo.core.util.GameState
import com.example.nsddemo.data.local.network.socket.Server
import com.example.nsddemo.data.repository.GameRepository
import com.example.nsddemo.data.util.InvalidStateException
import com.example.nsddemo.domain.legacy.ServerGameStateManager
import com.example.nsddemo.presentation.util.BaseGameStateHandler

class LobbyGameStateHandler(
    gameRepository: GameRepository,
    serverGameStateManager: ServerGameStateManager,
) : BaseGameStateHandler(gameRepository, serverGameStateManager) {
    override suspend fun handleGameStateChanges() {
        val isHost = gameRepository.gameData.value.isHost!!
        if (!isHost) {
            return
        }
        gameRepository.gameState.collect {
            when (it) {
                is GameState.StartGame -> {
                    // Ignore for now
                }

                is GameState.GetPlayerInfo -> {
                    // This is a hack to ignore the state(s) that were already handled in the
                    // previous screen (ChooseCategory screen)
                    if (it.connection !in Server.clients.keys) {
                        Log.d(TAG, "Handling GetPlayerInfo state")
                        serverGameStateManager.handleGetPlayerInfoState()
                    }
                }

                is GameState.DisplayCategoryAndWord -> {
                    serverGameStateManager.handleDisplayCategoryAndWordState()
                    _lastStateHandlerListener.value = true
                }

                is GameState.Transitioning -> {
                    if (it.to !is GameState.StartNewRound) {
                        throw InvalidStateException(it.to, gameRepository.screenAllowedStates.value)
                    }
                    gameRepository.updateGameState(it.to)
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