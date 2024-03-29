package com.example.nsddemo

import com.example.nsddemo.ui.PlayerColors
import com.example.nsddemo.ui.toPlayerColors

data class GameData(
    var gameCode: String? = null,
    var isHost: Boolean? = null,
    var currentPlayer: Player? = null,
    var imposter: Player? = null,
    var isFirstRound: Boolean = true,
    var categoryOrdinal: Int = -1,
    var wordResID: Int = -1,
    var isAsking: Boolean? = null,
    var currentPlayerPairIndex: Int = 0,
    val players: MutableList<Player> = mutableListOf(),
    val roundPlayerPairs: MutableList<Pair<Player, Player>> = mutableListOf(),
    val roundPlayerVotes: MutableMap<Player, Player> = mutableMapOf(),
    val roundVotingCounts: MutableMap<Player, Int> = mutableMapOf(),
    val playerScores: MutableMap<Player, Int> = mutableMapOf(),
) {
    val isImposter: Boolean?
        get() = if (currentPlayer == null || imposter == null) null else currentPlayer == imposter
    val selectedPlayerColors: List<PlayerColors>
        get() = players.map { it.color.toPlayerColors() }
}
