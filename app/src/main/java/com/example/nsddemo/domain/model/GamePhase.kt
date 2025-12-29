package com.example.nsddemo.domain.model

import kotlin.reflect.KClass

// 1. The Root FSM Interface
sealed interface GamePhase {
    val validNextStates: Set<KClass<out GamePhase>>

    fun checkTransitionOrThrow(newState: GamePhase) {
        if (!isTransitionValid(newState)) {
            throw IllegalStateException(
                "Invalid State Transition: Cannot go from ${this::class.simpleName} to ${newState::class.simpleName}"
            )
        }
    }

    private fun isTransitionValid(newState: GamePhase): Boolean {
        return newState::class in validNextStates
    }
}

// 2. The Marker Interfaces (Logical Grouping)
/** Indicates the app is connected to a session (Lobby or Game) */
interface Connected : GamePhase

/** Indicates the game loop is active (Prevent Back Button, Keep Screen On) */
interface InGame : GamePhase, Connected

// 3. The States
data object Idle : GamePhase {
    override val validNextStates = setOf(Lobby::class)
}

data object Lobby : GamePhase, Connected {
    override val validNextStates = setOf(Idle::class, CategorySelection::class)
}

data object CategorySelection : GamePhase, Connected {
    override val validNextStates = setOf(Lobby::class, RoleDistribution::class, Idle::class)
}

data object RoleDistribution : GamePhase, InGame {
    override val validNextStates = setOf(RoundQuestions::class, Idle::class)
}

data object RoundQuestions : GamePhase, InGame {
    override val validNextStates = setOf(RoundVoting::class, Idle::class)
}

data object RoundVoting : GamePhase, InGame {
    override val validNextStates = setOf(RoundResults::class, Idle::class)
}

data object RoundResults : GamePhase, InGame {
    override val validNextStates = setOf(Lobby::class, RoundQuestions::class, Idle::class)
    // Note: To "Replay", we go back to Lobby or start Questions again.
}