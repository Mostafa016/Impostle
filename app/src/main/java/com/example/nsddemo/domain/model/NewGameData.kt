package com.example.nsddemo.domain.model

import kotlinx.serialization.Serializable

@Serializable
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
    val category: GameCategory? = null,
    val word: String? = null,
    val readyPlayerIds: Set<String> = emptySet(),

    // Game Mode Specific Turn Management
    val roundData: RoundData = RoundData.Idle,

    // Voting State (VoterID -> TargetID)
    val votes: Map<String, String> = emptyMap(),

    // Scoreboard (PlayerID -> Score)
    val scores: Map<String, Int> = emptyMap(),

    // phase we were in before the FIRST disconnect
    val phaseBeforePause: GamePhase? = null
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

    val usedColors: Set<String>
        get() = players.values.map { it.color }.toSet()
    //endregion

    //region --- Role Assignment Helpers ---
    val readyPlayers: List<Player>
        get() = readyPlayerIds.map { players[it]!! }
    val readyCount: Int
        get() = readyPlayerIds.size
    val isLocalPlayerReady: Boolean
        get() = localPlayerId in readyPlayerIds
    //endregion

    //region --- Turn Helpers ---
    val isMyTurn: Boolean
        get() = roundData.isPlayerTurn(localPlayerId)
    //endregion

    //region --- Voting Helpers ---
    val voters: Set<String>
        get() = votes.keys

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

@Serializable
sealed interface RoundData {

    fun isPlayerTurn(playerId: String): Boolean

    @Serializable
    data object Idle : RoundData {
        override fun isPlayerTurn(playerId: String): Boolean = false
    }

    @Serializable
    data class QuestionRoundData(
        val roundPairs: List<Pair<String, String>> = emptyList(), // [(AskerID, TargetID), ...]
        val currentPairIndex: Int = 0,
    ) : RoundData {
        //region --- Turn Helpers ---
        val currentAskerId: String?
            get() = roundPairs.getOrNull(currentPairIndex)?.first
        val currentAskedId: String?
            get() = roundPairs.getOrNull(currentPairIndex)?.second
        val isLastQuestion: Boolean
            get() = roundPairs.isNotEmpty() && currentPairIndex == roundPairs.lastIndex

        override fun isPlayerTurn(playerId: String): Boolean {
            return playerId.isNotEmpty() && playerId == currentAskerId
        }
        //endregion
    }
}
