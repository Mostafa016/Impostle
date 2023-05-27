package com.example.nsddemo

sealed interface GameState {
    class GetPlayerName(val name: String) : GameState
    class DisplayCategoryAndWord(val category: String, val word: String) : GameState
    class AskQuestion(val asker: Player, val asked: Player, val isAsking: Boolean) :
        GameState

    object StartVote : GameState
    class EndVote(val votedPlayer: Player) : GameState
    object ShowScoreboard : GameState
    object Replay : GameState
}
