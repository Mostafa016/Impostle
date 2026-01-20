package com.example.nsddemo.domain.legacy

import com.example.nsddemo.domain.model.Categories
import com.example.nsddemo.domain.model.Player
import com.example.nsddemo.presentation.util.PlayerColors
import com.example.nsddemo.presentation.util.toPlayerColors

data class GameData(
    val gameCode: String? = null,
    val isHost: Boolean? = null,
    val currentPlayer: Player? = null,
    val imposter: Player? = null,
    val isFirstRound: Boolean = true,
    val categoryOrdinal: Int = -1,
    val wordResID: Int = -1,
    val currentPlayerPairIndex: Int = 0,
    val players: List<Player> = emptyList(),
    val roundPlayerPairs: List<Pair<Player, Player>> = emptyList(),
    val roundPlayerVotes: Map<Player, Player> = emptyMap(),
    val roundVotingCounts: Map<Player, Int> = emptyMap(),
    val playerScores: Map<Player, Int> = emptyMap(),
) {
    val playersExcludingCurrent: List<Player>
        get() = players.filter { it != currentPlayer }
    val category: Categories?
        get() = if (categoryOrdinal == -1) null else Categories.values()[categoryOrdinal]
    val isImposter: Boolean?
        get() = if (currentPlayer == null && imposter == null) {
            null
        } else if (imposter == null) {
            false
        } else {
            currentPlayer == imposter
        }
    val selectedPlayerColors: List<PlayerColors>
        get() = players.map { it.color.toPlayerColors() }
    val currentPlayerPair: Pair<Player, Player>
        get() = roundPlayerPairs[currentPlayerPairIndex]
    val isAsking: Boolean
        get() = currentPlayerPair.first == currentPlayer

    val isLastQuestion: Boolean
        get() = currentPlayerPairIndex == roundPlayerPairs.size - 1
    val numberOfPlayersWhoVoted: Int
        get() = roundPlayerVotes.size
    val currentPlayerVotedPlayer: Player?
        get() = roundPlayerVotes[currentPlayer]
}
