package com.example.nsddemo.domain.util

import com.example.nsddemo.domain.model.Player
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

@Serializable
sealed class Message(private val messageOrder: MessageOrder) {
    val order: Int
        get() = messageOrder.ordinal

//    companion object {
//        val json = Json
//
//        fun toJson(message: Message): String = json.encodeToString(message)
//
//        inline fun <reified T : Message> fromJson(jsonString: String): Message =
//            json.decodeFromString<T>(jsonString)
//    }
}

@Serializable
sealed class ServerMessage(private val globalOrder: MessageOrder) : Message(globalOrder) {
    @Serializable
    data class PlayerColor(val playerColor: String) :
        ServerMessage(MessageOrder.SERVER_PLAYER_COLOR)

    @Serializable
    data class IsLastPlayer(val isLastPlayer: Boolean) :
        ServerMessage(MessageOrder.SERVER_IS_LAST_PLAYER)

    @Serializable
    data class PlayerList(val playerList: List<Player>) :
        ServerMessage(MessageOrder.SERVER_PLAYER_LIST)

    @Serializable
    data class CategoryAndWord(val categoryOrdinal: Int, val wordResID: Int) :
        ServerMessage(MessageOrder.SERVER_CATEGORY_AND_WORD)

    @Serializable
    data class AskQuestion(
        val asker: Player,
        val asked: Player,
        val isAsking: Boolean,
        val isLastQuestion: Boolean,
        val isFirstQuestionInNewRound: Boolean,
    ) : ServerMessage(MessageOrder.SERVER_ASK_QUESTION)

    @Serializable
    data object EndOfQuestionsRound : ServerMessage(MessageOrder.SERVER_END_OF_QUESTIONS_ROUND)

    @Serializable
    data class ExtraQuestionsRound(val isExtraQuestionsRound: Boolean) :
        ServerMessage(MessageOrder.SERVER_IS_EXTRA_QUESTIONS_ROUND)

    @Serializable
    data object StartVote : ServerMessage(MessageOrder.SERVER_START_VOTE)

    @Serializable
    data class VotingResults(
        val votes: Map<String, Int>,
        val imposterPlayer: Player,
        val playerScores: Map<String, Int>
    ) : ServerMessage(MessageOrder.SERVER_VOTING_RESULTS)

    @Serializable
    data class ReplayDecision(val isReplayGame: Boolean) :
        ServerMessage(MessageOrder.SERVER_IS_REPLAY_GAME)
}

@Serializable
sealed class ClientMessage(private val globalOrder: MessageOrder) : Message(globalOrder) {
    data class RegisterPlayer(val playerName: String) :
        ClientMessage(MessageOrder.CLIENT_PLAYER_NAME)

    data object ConfirmRoleReceived :
        ClientMessage(MessageOrder.CLIENT_CATEGORY_AND_WORD_CONFIRMATION)

    data object ConfirmQuestionFinished : ClientMessage(MessageOrder.CLIENT_QUESTION_CONFIRMATION)
    data class SubmitVote(val vote: Player) : ClientMessage(MessageOrder.CLIENT_VOTE)
}

// Configure Json for Polymorphism (Crucial for sealed classes)
val NetworkJson = Json {
    prettyPrint = true // Good for debugging, disable for release
//    isLenient = true // Allows slightly malformed JSON, use with caution
//    ignoreUnknownKeys = true // Useful if client/server versions mismatch slightly
    serializersModule = SerializersModule {
        polymorphic(Message::class) {
            // Server Messages
            subclass(ServerMessage.PlayerColor::class)
            subclass(ServerMessage.IsLastPlayer::class)
            subclass(ServerMessage.PlayerList::class)
            subclass(ServerMessage.CategoryAndWord::class)
            subclass(ServerMessage.EndOfQuestionsRound::class)
            subclass(ServerMessage.ExtraQuestionsRound::class)
            subclass(ServerMessage.StartVote::class)
            subclass(ServerMessage.VotingResults::class)
            subclass(ServerMessage.ReplayDecision::class)
            // Client Messages
            subclass(ClientMessage.RegisterPlayer::class)
            subclass(ClientMessage.ConfirmRoleReceived::class)
            subclass(ClientMessage.ConfirmQuestionFinished::class)
            subclass(ClientMessage.SubmitVote::class)
        }
    }
}
