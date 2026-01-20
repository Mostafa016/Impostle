package com.example.nsddemo.domain.strategy

import com.example.nsddemo.domain.model.ClientMessage
import com.example.nsddemo.domain.model.GamePhase
import com.example.nsddemo.domain.model.GameStateTransition
import com.example.nsddemo.domain.model.NewGameData
import com.example.nsddemo.domain.model.RoundData

interface GameModeStrategy {
    val roundData: RoundData

    fun handleAction(
        data: NewGameData,
        phase: GamePhase,
        message: ClientMessage,
        playerID: String
    ): GameStateTransition
}