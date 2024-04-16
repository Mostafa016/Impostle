package com.example.nsddemo

import io.ktor.network.sockets.Connection

sealed interface GameState {
    sealed interface ClientGameState : GameState
    object StartGame : GameState {
        override fun toString(): String {
            return "StartGame"
        }
    }

    object ClientFoundGame : GameState, ClientGameState {
        override fun toString(): String {
            return "ClientFoundGame"
        }
    }

    object ClientGameStarted : GameState, ClientGameState {
        override fun toString(): String {
            return "ClientGameStarted"
        }
    }

    data class GetPlayerInfo(val name: String, val connection: Connection) : GameState
    data class DisplayCategoryAndWord(val categoryOrdinal: Int, val wordResourceId: Int) : GameState

    sealed interface ConfirmReadCategoryAndWord : GameState {
        val numberOfConfirmations: Int
    }

    data class GetPlayerReadCategoryAndWordConfirmation(override val numberOfConfirmations: Int = 0) :
        GameState, ConfirmReadCategoryAndWord

    data class ConfirmCurrentPlayerReadCategoryAndWord(override val numberOfConfirmations: Int = 0) :
        GameState, ConfirmReadCategoryAndWord

    data class AskQuestion(
        val asker: Player,
        val asked: Player,
        val isAsking: Boolean,
        val isLastQuestion: Boolean,
        val isFirstQuestionInNewRound: Boolean = false
    ) : GameState

    data class ConfirmCurrentPlayerQuestion(val currentAskQuestionState: AskQuestion) : GameState

    object ChooseExtraQuestions : GameState {
        override fun toString(): String {
            return "ChooseExtraQuestions"
        }
    }

    object AskExtraQuestions : GameState {
        override fun toString(): String {
            return "AskExtraQuestions"
        }
    }

    object StartVote : GameState {
        override fun toString(): String {
            return "StartVote"
        }
    }

    data class GetPlayerVote(val voter: Player, val voted: Player) : GameState
    data class GetCurrentPlayerVote(val voted: Player) : GameState
    data class EndVote(val topVotedPlayer: Player) : GameState
    object ShowScoreboard : GameState {
        override fun toString(): String {
            return "ShowScoreboard"
        }
    }

    data class Replay(val replay: Boolean) : GameState
}
