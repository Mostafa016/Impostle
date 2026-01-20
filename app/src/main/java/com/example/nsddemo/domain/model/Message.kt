package com.example.nsddemo.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

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
    data object GameResumed : ServerMessage()

    @Serializable
    data class ReconnectionFullStateSync(val data: NewGameData, val phase: GamePhase) :
        ServerMessage()

    @Serializable
    data object GameFull : ServerMessage()

    @Serializable
    data object GameAlreadyStarted : ServerMessage()
    //endregion

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
    data object ReplayRound : ServerMessage()

    @Serializable
    data object StartVote : ServerMessage()

    @Serializable
    data class PlayerVoted(val playerId: String, val votedPlayerId: String) : ServerMessage()

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
    serializersModule = SerializersModule {
        polymorphic(Message::class) {
            //region Server Messages
            subclass(ServerMessage.PlayerDisconnected::class)
            subclass(ServerMessage.PlayerReconnected::class)
            subclass(ServerMessage.GameResumed::class)
            subclass(ServerMessage.ReconnectionFullStateSync::class)
            subclass(ServerMessage.GameFull::class)
            subclass(ServerMessage.GameAlreadyStarted::class)
            subclass(ServerMessage.PlayerList::class)
            subclass(ServerMessage.CategorySelected::class)
            subclass(ServerMessage.RoleAssigned::class)
            subclass(ServerMessage.PlayerReady::class)
            subclass(ServerMessage.Question::class)
            subclass(ServerMessage.RoundEnd::class)
            subclass(ServerMessage.ReplayRound::class)
            subclass(ServerMessage.StartVote::class)
            subclass(ServerMessage.PlayerVoted::class)
            subclass(ServerMessage.VoteResult::class)
            subclass(ServerMessage.ContinueToGameChoice::class)
            subclass(ServerMessage.ReplayGame::class)
            subclass(ServerMessage.EndGame::class)
            //endregion
            //region Client Messages
            subclass(ClientMessage.RegisterPlayer::class)
            subclass(ClientMessage.RequestSelectCategory::class)
            subclass(ClientMessage.RequestStartGame::class)
            subclass(ClientMessage.ConfirmRoleReceived::class)
            subclass(ClientMessage.EndTurn::class)
            subclass(ClientMessage.RequestReplayRound::class)
            subclass(ClientMessage.RequestStartVote::class)
            subclass(ClientMessage.SubmitVote::class)
            subclass(ClientMessage.RequestContinueToGameChoice::class)
            subclass(ClientMessage.RequestReplayGame::class)
            subclass(ClientMessage.RequestEndGame::class)
            //endregion
        }
    }
}