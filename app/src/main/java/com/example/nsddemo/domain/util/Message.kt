package com.example.nsddemo.domain.util

import com.example.nsddemo.domain.model.Player
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
sealed class Message(private val messageOrder: MessageOrder) {
    val order: Int
        get() = messageOrder.ordinal

    companion object {
        val json = Json

        fun toJson(message: Message): String = json.encodeToString(message)

        inline fun <reified T : Message> fromJson(jsonString: String): Message =
            json.decodeFromString<T>(jsonString)
    }
}

sealed class ServerMessage(messageOrder: MessageOrder) : Message(messageOrder) {
    data class PlayerColor(val playerColor: String) :
        ServerMessage(MessageOrder.SERVER_PLAYER_COLOR)

    data class IsLastPlayer(val isLastPlayer: Boolean) :
        ServerMessage(MessageOrder.SERVER_IS_LAST_PLAYER)

    data class PlayerList(val playerList: List<Player>) :
        ServerMessage(MessageOrder.SERVER_PLAYER_LIST)

    data class CategoryAndWord(val categoryOrdinal: Int, val wordResID: Int) :
        ServerMessage(MessageOrder.SERVER_CATEGORY_AND_WORD)

    data class AskQuestion(
        val asker: Player,
        val asked: Player,
        val isAsking: Boolean,
        val isLastQuestion: Boolean,
        val isFirstQuestionInNewRound: Boolean,
    ) : ServerMessage(MessageOrder.SERVER_ASK_QUESTION)

    data object EndOfQuestionsRound : ServerMessage(MessageOrder.SERVER_END_OF_QUESTIONS_ROUND)
    data class ExtraQuestionsRound(val isExtraQuestionsRound: Boolean) :
        ServerMessage(MessageOrder.SERVER_IS_EXTRA_QUESTIONS_ROUND)

    data object StartVote : ServerMessage(MessageOrder.SERVER_START_VOTE)
    data class Imposter(val imposter: Player) : ServerMessage(MessageOrder.SERVER_IMPOSTER)
    data class VotingResults(val votingResults: Map<String, Int>) :
        ServerMessage(MessageOrder.SERVER_VOTING_RESULTS)

    data class PlayerScores(val playerScores: Map<String, Int>) :
        ServerMessage(MessageOrder.SERVER_PLAYER_SCORES)

    data class ReplayGame(val isReplayGame: Boolean) :
        ServerMessage(MessageOrder.SERVER_IS_REPLAY_GAME)
}

sealed class ClientMessage(messageOrder: MessageOrder) : Message(messageOrder) {
    data class PlayerName(val playerName: String) : ClientMessage(MessageOrder.CLIENT_PLAYER_NAME)
    data object CategoryAndWordConfirmation :
        ClientMessage(MessageOrder.CLIENT_CATEGORY_AND_WORD_CONFIRMATION)

    data object QuestionConfirmation : ClientMessage(MessageOrder.CLIENT_QUESTION_CONFIRMATION)
    data class ClientVote(val vote: Player) : ClientMessage(MessageOrder.CLIENT_VOTE)
}
