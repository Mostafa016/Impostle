package com.example.nsddemo.domain.model

import com.example.nsddemo.domain.util.Categories

data class NewGameData(
    // --- Identity ---
    val localPlayerId: String = "",
    val hostId: String = "",
    val gameCode: String = "",

    // --- Player Database (PlayerID -> Player)---
    val players: Map<String, Player> = emptyMap(),

    // --- Game Logic State ---
    val roundNumber: Int = 1,
    val imposterId: String? = null,
    val category: Categories? = null,
    val wordKey: String? = null, // String Key (e.g. "LION"), NOT Resource ID

    // Turn Management (AskerID -> AskedID)
    val roundPairs: List<Pair<String, String>> = emptyList(), // [(AskerID, TargetID), ...]
    val currentPairIndex: Int = 0,

    // Voting State (VoterID -> TargetID)
    val votes: Map<String, String> = emptyMap(),

    // Scoreboard (PlayerID -> Score)
    val scores: Map<String, Int> = emptyMap()
) {
    //region --- Identity Helpers ---
    val localPlayer: Player?
        get() = players[localPlayerId]

    val isHost: Boolean
        get() = localPlayerId.isNotEmpty() && localPlayerId == hostId

    val isImposter: Boolean
        get() = localPlayerId.isNotEmpty() && localPlayerId == imposterId

    val otherPlayers: List<Player>
        get() = players.values.filter { it.id != localPlayerId }

    val usedColors: Set<String> // Or Set<PlayerColors> if mapped
        get() = players.values.map { it.color }.toSet()
    //endregion

    //region --- Turn Helpers ---
    val currentAskerId: String?
        get() = roundPairs.getOrNull(currentPairIndex)?.first
    val currentAskedId: String?
        get() = roundPairs.getOrNull(currentPairIndex)?.second
    val isLocalPlayerAsking: Boolean
        get() = localPlayerId.isNotEmpty() && localPlayerId == currentAskerId
    val isLastQuestion: Boolean
        get() = roundPairs.isNotEmpty() && currentPairIndex == roundPairs.lastIndex
    //endregion

    //region --- Voting Helpers ---
    val voteCounts: Map<String, Int>
        get() = votes.values.groupingBy { it }.eachCount()

    val voteCountsAsPlayers: Map<Player, Int>
        get() = voteCounts.mapKeys { players[it.key]!! }

    val hasLocalPlayerVoted: Boolean
        get() = votes.containsKey(localPlayerId)

    val localPlayerVotedTargetId: String?
        get() = votes[localPlayerId]

    val numberOfPlayersWhoVoted: Int
        get() = votes.size

    val hasEveryoneVoted: Boolean
        get() = players.isNotEmpty() && votes.size == players.size
    //endregion

    //region --- Round Helpers ---
    val isFirstRound: Boolean
        get() = roundNumber == 1
    //endregion
}
