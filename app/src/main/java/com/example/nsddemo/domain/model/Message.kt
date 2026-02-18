package com.example.nsddemo.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
sealed class Message

@Serializable
sealed class ServerMessage : Message() {
    //region Session Management
    @Serializable
    data class PlayerDisconnected(val playerId: String) :
        ServerMessage()

    @Serializable
    data class PlayerReconnected(val player: Player) :
        ServerMessage()

    @Serializable
    data class GameResumed(val phaseAfterPause: GamePhase) : ServerMessage()

    @Serializable
    data class ReconnectionFullStateSync(val data: GameData, val phase: GamePhase) :
        ServerMessage()

    @Serializable
    data object GameFull : ServerMessage()

    @Serializable
    data object GameAlreadyStarted : ServerMessage()

    // Graceful exit signals
    @Serializable
    data object YouWereKicked : ServerMessage()

    @Serializable
    data object LobbyClosed : ServerMessage()
    //endregion

    @Serializable
    data class RegisterHost(val hostId: String) : ServerMessage()

    @Serializable
    data class PlayerList(val players: List<Player>) :
        ServerMessage()

    @Serializable
    data class CategorySelected(val category: GameCategory) :
        ServerMessage()

    @Serializable
    data class RoleAssigned(val category: GameCategory, val word: String) :
        ServerMessage()

    @Serializable
    data class PlayerReady(val readyPlayerIds: List<String>) :
        ServerMessage()

    @Serializable
    data class Question(
        val askerId: String,
        val askedId: String,
    ) : ServerMessage()

    @Serializable
    data object RoundEnd : ServerMessage()

    @Serializable
    data class ReplayRound(val incrementRoundNumber: Boolean = true) : ServerMessage()

    @Serializable
    data object StartVote : ServerMessage()

    @Serializable
    data class PlayerVoted(val playerId: String, val votedPlayerId: String) : ServerMessage()

    @Serializable
    data class VotesAfterLeaver(val votes: Map<String, String>) : ServerMessage()

    @Serializable
    data class ScoresAfterLeaver(val scores: Map<String, Int>) : ServerMessage()

    @Serializable
    data class VoteResult(
        val voteResult: Map<String, String>,
        val imposterId: String,
        val playerScores: Map<String, Int>
    ) : ServerMessage()

    @Serializable
    data object ContinueToGameChoice : ServerMessage()

    @Serializable
    data object ReplayGame :
        ServerMessage()

    @Serializable
    data object EndGame : ServerMessage()
}

@Serializable
sealed class ClientMessage : Message() {
    @Serializable
    data class RegisterPlayer(val playerName: String, val playerId: String) :
        ClientMessage()

    //region Host-as-a-client messages
    @Serializable
    data class RequestSelectCategory(val category: GameCategory) :
        ClientMessage()

    @Serializable
    data class RequestKickPlayer(val playerId: String) : ClientMessage()

    @Serializable
    data object RequestStartGame : ClientMessage()
    //endregion

    @Serializable
    data object ConfirmRoleReceived :
        ClientMessage()

    @Serializable
    data object EndTurn : ClientMessage()

    //region Host-as-a-client messages
    @Serializable
    data object RequestReplayRound : ClientMessage()

    @Serializable
    data object RequestStartVote : ClientMessage()
    //endregion

    @Serializable
    data class SubmitVote(val votedPlayerID: String) : ClientMessage()

    //region Host-as-a-client messages
    @Serializable
    data object RequestContinueToGameChoice :
        ClientMessage()

    @Serializable
    data object RequestReplayGame : ClientMessage()

    @Serializable
    data object RequestEndGame : ClientMessage()
    //endregion
}

// Configure Json
val NetworkJson = Json {
    prettyPrint = true
//    ignoreUnknownKeys = true // Useful if client/server versions mismatch slightly

}