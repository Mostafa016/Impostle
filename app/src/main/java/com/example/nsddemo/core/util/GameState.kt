package com.example.nsddemo.core.util

import com.example.nsddemo.domain.model.Player
import io.ktor.network.sockets.Connection
import kotlin.reflect.KClass

sealed interface GameState {
    sealed interface ClientGameState : GameState
    data object ClientFoundGame : GameState, ClientGameState
    data object ClientGameStarted : GameState, ClientGameState

    data object StartGame : GameState
    data class GetPlayerInfo(val name: String, val connection: Connection) : GameState
    data object StartNewRound : GameState
    data class DisplayCategoryAndWord(val categoryOrdinal: Int, val wordResourceId: Int) : GameState

    sealed interface ConfirmReadCategoryAndWord : GameState {
        val numberOfConfirmations: Int
    }

    data class GetPlayerReadCategoryAndWordConfirmation(override val numberOfConfirmations: Int = 0) :
        GameState, ConfirmReadCategoryAndWord

    data class ConfirmCurrentPlayerReadCategoryAndWord(override val numberOfConfirmations: Int = 0) :
        GameState, ConfirmReadCategoryAndWord

    data class AskQuestion(
        val asker: Player,
        val asked: Player,
        val isAsking: Boolean,
        val isLastQuestion: Boolean,
        val isFirstQuestionInNewRound: Boolean = false
    ) : GameState

    data class ConfirmCurrentPlayerQuestion(val currentAskQuestionState: AskQuestion) : GameState

    data object ChooseExtraQuestions : GameState

    data object AskExtraQuestions : GameState

    data object StartVote : GameState

    data class GetPlayerVote(val voter: Player, val voted: Player) : GameState
    data class GetCurrentPlayerVote(val voted: Player) : GameState
    data class EndVote(val topVotedPlayer: Player) : GameState
    data object ShowScoreboard : GameState

    // TODO: Split into 3 states (ChooseEndGameOrReplay, EndGame, Replay)
    data class Replay(val replay: Boolean) : GameState

    data class Transitioning(val from: GameState, val to: GameState) : GameState

    val GameState.validNextStates: Set<KClass<out GameState>>
        get() = when (this) {
            is StartGame -> setOf(GetPlayerInfo::class)
            is GetPlayerInfo -> setOf(
                GetPlayerInfo::class, DisplayCategoryAndWord::class
            )

            is StartNewRound -> setOf(DisplayCategoryAndWord::class)
            is DisplayCategoryAndWord -> setOf(
                GetPlayerReadCategoryAndWordConfirmation::class,
                ConfirmCurrentPlayerReadCategoryAndWord::class
            )

            is GetPlayerReadCategoryAndWordConfirmation -> setOf(
                GetPlayerReadCategoryAndWordConfirmation::class,
                ConfirmCurrentPlayerReadCategoryAndWord::class,
                AskQuestion::class
            )

            is ConfirmCurrentPlayerReadCategoryAndWord -> setOf(
                GetPlayerReadCategoryAndWordConfirmation::class
            )

            is AskQuestion -> setOf(
                AskQuestion::class, ConfirmCurrentPlayerQuestion::class, ChooseExtraQuestions::class
            )

            is ConfirmCurrentPlayerQuestion -> setOf(
                AskQuestion::class, ChooseExtraQuestions::class
            )

            ChooseExtraQuestions -> setOf(AskExtraQuestions::class, StartVote::class)
            AskExtraQuestions -> setOf(AskQuestion::class)
            StartVote -> setOf(GetPlayerVote::class, GetCurrentPlayerVote::class)
            is GetPlayerVote -> setOf(
                GetPlayerVote::class, GetCurrentPlayerVote::class, EndVote::class
            )

            is GetCurrentPlayerVote -> setOf(GetPlayerVote::class)
            is EndVote -> setOf(ShowScoreboard::class, Replay::class, Transitioning::class)
            ShowScoreboard -> setOf(Replay::class, Transitioning::class)
            is Replay -> setOf(Transitioning::class)
            is Transitioning -> emptySet()

            is ClientGameStarted -> setOf(ClientFoundGame::class)
            is ClientFoundGame -> setOf(ClientGameStarted::class)
        }
}
