package com.example.nsddemo

import io.ktor.network.sockets.Connection

sealed interface GameState {
    object StartGame : GameState
    class GetPlayerInfo(val name: String, val connection: Connection) : GameState
    class DisplayCategoryAndWord(val category: String, val word: String) : GameState
    class AskQuestion(val asker: Player, val asked: Player, val isAsking: Boolean) :
        GameState

    object StartVote : GameState
    class GetPlayerVote(val voter: Player, val voted: Player) : GameState
    class EndVote(val votedPlayer: Player) : GameState
    object ShowScoreboard : GameState
    object Replay : GameState
}
