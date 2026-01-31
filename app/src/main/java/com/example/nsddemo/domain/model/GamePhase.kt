package com.example.nsddemo.domain.model

import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

// 1. The Root FSM Interface

//region Marker Interfaces (Logical Grouping)
/** Indicates the app is connected to a session (Lobby or Game) */
interface Connected : GamePhase

/** Indicates the game loop is active (Prevent Back Button, Keep Screen On) */
interface InGame : GamePhase, Connected

/**
 * Marker for phases where a missing player breaks the game flow.
 * Disconnects here trigger PAUSE. Disconnects elsewhere trigger DELETE.
 */
interface Active : GamePhase, InGame
//endregion

@Serializable
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

    //region Phases
    @Serializable
    data object Idle : GamePhase {
        override val validNextStates = setOf(Lobby::class)
    }

    @Serializable
    data object Lobby : GamePhase, Connected {
        override val validNextStates = setOf(Idle::class, RoleDistribution::class)
    }

    @Serializable
    data object RoleDistribution : GamePhase, Active {
        override val validNextStates = setOf(InRound::class, Idle::class, Paused::class)
    }

    @Serializable
    data object InRound : GamePhase, Active {
        override val validNextStates = setOf(RoundReplayChoice::class, Idle::class, Paused::class)
    }

    @Serializable
    data object RoundReplayChoice : GamePhase, Active {
        override val validNextStates =
            setOf(GameVoting::class, InRound::class, Idle::class, Paused::class)
    }

    @Serializable
    data object GameVoting : GamePhase, Active {
        override val validNextStates = setOf(GameResults::class, Idle::class, Paused::class)
    }

    @Serializable
    data object GameResults : GamePhase, Active {
        override val validNextStates = setOf(GameReplayChoice::class, Idle::class, Paused::class)
    }

    @Serializable
    data object GameReplayChoice : GamePhase, Active {
        override val validNextStates = setOf(Lobby::class, Idle::class, Paused::class)
    }

    @Serializable
    data object Paused : GamePhase, Connected {
        override val validNextStates: Set<KClass<out GamePhase>>
            get() = setOf(
                // Resuming
                RoleDistribution::class,
                InRound::class,
                RoundReplayChoice::class,
                GameVoting::class,
                GameResults::class,

                // End game
                Idle::class
            )
    }
    //endregion
}

