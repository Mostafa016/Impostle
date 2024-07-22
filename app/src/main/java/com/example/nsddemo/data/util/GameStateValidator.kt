package com.example.nsddemo.data.util

import com.example.nsddemo.core.util.GameState
import com.example.nsddemo.core.util.GameState.ClientFoundGame.validNextStates


object GameStateValidator {
    fun validateState(from: GameState, to: GameState, screenAllowedStates: Set<String>) {
        if (!isValidTransition(from, to)) {
            throw InvalidStateTransitionException(from, to)
        }
        if (!isStateAllowedInScreen(to, screenAllowedStates)) {
            throw InvalidStateException(to, screenAllowedStates)
        }
    }

    private fun isValidTransition(from: GameState, to: GameState): Boolean =
        to::class in from.validNextStates

    private fun isStateAllowedInScreen(
        to: GameState, screenAllowedStates: Set<String>
    ): Boolean =
        to::class.simpleName in screenAllowedStates


}

class InvalidStateTransitionException(from: GameState, to: GameState) :
    Exception("Invalid state transition from $from to $to")

class InvalidStateException(state: GameState, allowedStates: Set<String>) :
    Exception("Invalid state $state. Allowed states are $allowedStates")

