package com.example.nsddemo.presentation.screen.choose_category

import android.util.Log
import com.example.nsddemo.core.util.Debugging.TAG
import com.example.nsddemo.core.util.GameState
import com.example.nsddemo.data.local.network.socket.Server
import com.example.nsddemo.data.repository.GameRepository
import com.example.nsddemo.data.util.InvalidStateException
import com.example.nsddemo.domain.legacy.ServerGameStateManager
import com.example.nsddemo.presentation.util.BaseGameStateHandler

class ChooseCategoryGameStateHandler(
    gameRepository: GameRepository,
    serverGameStateManager: ServerGameStateManager
) : BaseGameStateHandler(gameRepository, serverGameStateManager) {
    override suspend fun handleGameStateChanges() {
        gameRepository.gameState.collect {
            when (it) {
                is GameState.StartGame -> {
                    // Do nothing
                    // This is allowed since you can choose a category before anyone joins
                }

                is GameState.GetPlayerInfo -> {
                    // This is a hack to ignore the state(s) that were already handled in the
                    // previous screen (Lobby screen)
                    if (it.connection !in Server.clients.keys) {
                        Log.d(TAG, "Handling GetPlayerInfo state")
                        serverGameStateManager.handleGetPlayerInfoState()
                    }
                }

                is GameState.DisplayCategoryAndWord -> {
                    // TODO: This is to avoid some race condition
                    // serverGameStateManager.handleDisplayCategoryAndWordState()
                }

                is GameState.StartNewRound -> {
                    // Do nothing
                }

                else -> {
                    // TODO: This way of handling state changes can cause race conditions
                    //  How: state updates happen independently of state checks in screens
                    //  Possible fix: Just remove state checking in screens
                    //  Only the sequence of events is important; already checked in validNextStates
                    //  **BUT** some screens require certain states before navigating to it,
                    //  e.x., ask question, so need a way to guarantee that
                    throw InvalidStateException(it, gameRepository.screenAllowedStates.value)
                }
            }
        }
    }
}