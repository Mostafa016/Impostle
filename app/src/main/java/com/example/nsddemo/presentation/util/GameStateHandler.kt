package com.example.nsddemo.presentation.util

import com.example.nsddemo.data.repository.GameRepository
import com.example.nsddemo.domain.legacy.ServerGameStateManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface GameStateHandler {
    // TODO: All handlers should be renamed and moved to use cases
    val lastStateHandlerListener: StateFlow<Boolean>
    suspend fun handleGameStateChanges()
}

abstract class BaseGameStateHandler(
    protected val gameRepository: GameRepository,
    protected val serverGameStateManager: ServerGameStateManager
) : GameStateHandler {
    protected val _lastStateHandlerListener = MutableStateFlow(false)
    override val lastStateHandlerListener: StateFlow<Boolean> =
        _lastStateHandlerListener.asStateFlow()
}


