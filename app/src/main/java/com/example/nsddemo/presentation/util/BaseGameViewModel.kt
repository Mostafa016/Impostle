package com.example.nsddemo.presentation.util

import androidx.lifecycle.ViewModel
import com.example.nsddemo.domain.engine.GameSession
import com.example.nsddemo.domain.model.GameData
import com.example.nsddemo.domain.model.GamePhase
import kotlinx.coroutines.flow.StateFlow

abstract class BaseGameViewModel(
    protected val gameSession: GameSession
) : ViewModel() {

    val gameData: StateFlow<GameData> = gameSession.gameData
    val gamePhase: StateFlow<GamePhase> = gameSession.gamePhase

    /**
     * Helper to access the active client for sending actions.
     * Safe to use as it returns null if no client is active.
     */
    protected val activeClient
        get() = gameSession.activeClient
}