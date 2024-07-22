package com.example.nsddemo.presentation.screen.choose_category

import android.util.Log
import com.example.nsddemo.core.util.Debugging.TAG
import com.example.nsddemo.core.util.GameState
import com.example.nsddemo.data.local.network.socket.Server
import com.example.nsddemo.data.repository.GameRepository
import com.example.nsddemo.data.util.InvalidStateException
import com.example.nsddemo.domain.use_case.ServerGameStateManager
import com.example.nsddemo.presentation.util.BaseGameStateHandler

class ChooseCategoryGameStateHandler(
    gameRepository: GameRepository,
    serverGameStateManager: ServerGameStateManager
) : BaseGameStateHandler(gameRepository, serverGameStateManager) {
    override suspend fun handleGameStateChanges() {
        gameRepository.gameState.collect {
            when (it) {
                is GameState.GetPlayerInfo -> {
                    // This is a hack to ignore the state(s) that were already handled in the
                    // previous screen (Lobby screen)
                    if (it.connection !in Server.clients.keys) {
                        Log.d(TAG, "Handling GetPlayerInfo state")
                        serverGameStateManager.handleGetPlayerInfoState()
                    }
                }

                else -> {
                    throw InvalidStateException(it, gameRepository.screenAllowedStates.value)
                }
            }
        }
    }
}