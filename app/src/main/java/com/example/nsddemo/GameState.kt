package com.example.nsddemo

import io.ktor.network.sockets.Connection

sealed interface GameState {
    object StartGame : GameState {
        override fun toString(): String {
            return "StartGame"
        }
    }

    data class GetPlayerInfo(val name: String, val connection: Connection) : GameState
    data class DisplayCategoryAndWord(val categoryOrdinal: Int, val wordResourceId: Int) : GameState
    data class GetPlayerReadCategoryAndWordConfirmation(val numberOfConfirmations: Int = 0) :
        GameState

    data class AskQuestion(
        val asker: Player,
        val asked: Player,
        val isAsking: Boolean,
        val isLastQuestion: Boolean,
        val isFirstQuestionInNewRound: Boolean = false
    ) :
        GameState

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
    data class EndVote(val topVotedPlayer: Player) : GameState
    object ShowScoreboard : GameState {
        override fun toString(): String {
            return "ShowScoreboard"
        }
    }

    data class Replay(val replay: Boolean) : GameState
}
