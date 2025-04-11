package com.example.nsddemo.domain.util

import com.example.nsddemo.domain.model.Player
import kotlinx.serialization.Serializable

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
    data class Imposter(val imposter: Player) : ServerMessage(MessageOrder.SERVER_IMPOSTER)

    @Serializable
    data class VotingResults(val votingResults: Map<String, Int>) :
        ServerMessage(MessageOrder.SERVER_VOTING_RESULTS)

    @Serializable
    data class PlayerScores(val playerScores: Map<String, Int>) :
        ServerMessage(MessageOrder.SERVER_PLAYER_SCORES)

    @Serializable
    data class ReplayGame(val isReplayGame: Boolean) :
        ServerMessage(MessageOrder.SERVER_IS_REPLAY_GAME)
}

@Serializable
sealed class ClientMessage(private val globalOrder: MessageOrder) : Message(globalOrder) {
    data class PlayerName(val playerName: String) : ClientMessage(MessageOrder.CLIENT_PLAYER_NAME)
    data object CategoryAndWordConfirmation :
        ClientMessage(MessageOrder.CLIENT_CATEGORY_AND_WORD_CONFIRMATION)

    data object QuestionConfirmation : ClientMessage(MessageOrder.CLIENT_QUESTION_CONFIRMATION)
    data class ClientVote(val vote: Player) : ClientMessage(MessageOrder.CLIENT_VOTE)
}
