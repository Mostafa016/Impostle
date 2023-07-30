package com.example.nsddemo

import io.ktor.network.sockets.Connection

sealed interface GameState {
    object StartGame : GameState
    class GetPlayerInfo(val name: String, val connection: Connection) : GameState
    class DisplayCategoryAndWord(val category: String, val word: String) : GameState
    class GetPlayerReadCategoryAndWordConfirmation(val numberOfConfirmations: Int) : GameState

    data class AskQuestion(
        val asker: Player,
        val asked: Player,
        val isAsking: Boolean,
        val isLastQuestion: Boolean
    ) :
        GameState

    object AskExtraQuestions : GameState

    object StartVote : GameState
    class GetPlayerVote(val voter: Player, val voted: Player) : GameState
    class EndVote(val votedPlayer: Player) : GameState
    object ShowScoreboard : GameState
    class Replay(val replay: Boolean) : GameState
}
