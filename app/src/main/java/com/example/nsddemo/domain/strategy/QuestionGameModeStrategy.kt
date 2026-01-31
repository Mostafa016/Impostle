package com.example.nsddemo.domain.strategy

import com.example.nsddemo.domain.logic.RoundPlayerPairsGenerator
import com.example.nsddemo.domain.model.Envelope
import com.example.nsddemo.domain.model.GamePhase
import com.example.nsddemo.domain.model.GameStateTransition
import com.example.nsddemo.domain.model.NewGameData
import com.example.nsddemo.domain.model.RoundData
import com.example.nsddemo.domain.model.ServerMessage
import com.example.nsddemo.domain.repository.WordRepository
import javax.inject.Inject

class QuestionGameModeStrategy @Inject constructor(wordRepository: WordRepository) :
    BaseGameModeStrategy(wordRepository) {
    override val roundData: RoundData
        get() = RoundData.QuestionRoundData()

    override fun setupRoundSpecifics(data: NewGameData): NewGameData {
        return data.copy(
            roundData = RoundData.QuestionRoundData(
                roundPairs = RoundPlayerPairsGenerator
                    .generate(data.players.values.toList())
                    .map { it.first.id to it.second.id })
        )
    }

    override fun onRoundStart(data: NewGameData): GameStateTransition {
        val roundData = data.roundData as? RoundData.QuestionRoundData
            ?: return GameStateTransition.Invalid("Round Data is not QuestionRoundData (It is ${data.roundData})")

        return GameStateTransition.Valid(
            data,
            GamePhase.InRound,
            listOf(
                Envelope.Broadcast(
                    ServerMessage.Question(
                        roundData.currentAskerId!!,
                        roundData.currentAskedId!!,
                    )
                )
            )
        )
    }

    override fun onTurnEnd(data: NewGameData, playerID: String): GameStateTransition {
        val currentRoundData = data.roundData as RoundData.QuestionRoundData
        if (currentRoundData.currentAskerId != playerID) {
            return GameStateTransition.Invalid("Only the asking player can end turn: askerId: ${currentRoundData.currentAskerId} playerID: $playerID")
        }

        if (currentRoundData.isLastQuestion) {
            return GameStateTransition.Valid(
                newGameData = data,
                newPhase = GamePhase.RoundReplayChoice,
                envelopes = listOf(Envelope.Broadcast(ServerMessage.RoundEnd))
            )
        }

        val updatedData =
            data.copy(roundData = currentRoundData.copy(currentPairIndex = currentRoundData.currentPairIndex + 1))
        val updatedRoundData = updatedData.roundData as RoundData.QuestionRoundData
        return GameStateTransition.Valid(
            updatedData,
            envelopes = listOf(
                Envelope.Broadcast(
                    ServerMessage.Question(
                        updatedRoundData.currentAskerId!!,
                        updatedRoundData.currentAskedId!!,
                    )
                )
            )
        )
    }
}