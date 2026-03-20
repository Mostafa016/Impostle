package com.mostafa.impostle.domain.strategy

import com.mostafa.impostle.domain.logic.RoundPlayerPairsGenerator
import com.mostafa.impostle.domain.model.Envelope
import com.mostafa.impostle.domain.model.GameData
import com.mostafa.impostle.domain.model.GamePhase
import com.mostafa.impostle.domain.model.GameStateTransition
import com.mostafa.impostle.domain.model.RoundData
import com.mostafa.impostle.domain.model.ServerMessage
import com.mostafa.impostle.domain.repository.WordRepository
import javax.inject.Inject

class QuestionGameModeStrategy
    @Inject
    constructor(
        wordRepository: WordRepository,
    ) : BaseGameModeStrategy(wordRepository) {
        override val roundData: RoundData
            get() = RoundData.QuestionRoundData()

        override fun setupRoundSpecifics(data: GameData): GameData =
            data.copy(
                roundData =
                    RoundData.QuestionRoundData(
                        roundPairs =
                            RoundPlayerPairsGenerator
                                .generate(data.players.values.toList())
                                .map { it.first.id to it.second.id },
                    ),
            )

        override fun onRoundStart(data: GameData): GameStateTransition {
            val roundData =
                data.roundData as? RoundData.QuestionRoundData
                    ?: return GameStateTransition.Invalid("Round Data is not QuestionRoundData (It is ${data.roundData})")

            return GameStateTransition.Valid(
                data,
                GamePhase.InRound,
                listOf(
                    Envelope.Broadcast(
                        ServerMessage.Question(
                            roundData.currentAskerId!!,
                            roundData.currentAskedId!!,
                        ),
                    ),
                ),
            )
        }

        override fun onTurnEnd(
            data: GameData,
            playerID: String,
        ): GameStateTransition {
            val currentRoundData = data.roundData as RoundData.QuestionRoundData
            if (currentRoundData.currentAskerId != playerID) {
                return GameStateTransition.Invalid(
                    "Only the asking player can end turn: askerId: ${currentRoundData.currentAskerId} playerID: $playerID",
                )
            }

            if (currentRoundData.isLastQuestion) {
                return GameStateTransition.Valid(
                    newGameData = data,
                    newPhase = GamePhase.RoundReplayChoice,
                    envelopes = listOf(Envelope.Broadcast(ServerMessage.RoundEnd)),
                )
            }

            val updatedData =
                data.copy(roundData = currentRoundData.copy(currentPairIndex = currentRoundData.currentPairIndex + 1))
            val updatedRoundData = updatedData.roundData as RoundData.QuestionRoundData
            return GameStateTransition.Valid(
                updatedData,
                envelopes =
                    listOf(
                        Envelope.Broadcast(
                            ServerMessage.Question(
                                updatedRoundData.currentAskerId!!,
                                updatedRoundData.currentAskedId!!,
                            ),
                        ),
                    ),
            )
        }

        override fun onPlayerRemoved(
            data: GameData,
            phase: GamePhase,
            removedPlayerId: String,
        ): GameStateTransition {
            // Civilian Kicked - Handle based on Phase
            val remainingPlayers = data.players
            return when (phase) {
                is GamePhase.RoleDistribution -> {
                    // If someone leaves during role assignment, the setup is invalid. Restart Game Setup.
                    GameStateTransition.Valid(
                        newGameData = data.copy(readyPlayerIds = emptySet()),
                        newPhase = GamePhase.Lobby, // Fallback to Lobby to pick category/start again
                        envelopes =
                            listOf(
                                Envelope.Broadcast(ServerMessage.PlayerList(remainingPlayers.values.toList())),
                                Envelope.Broadcast(ServerMessage.ReplayGame), // Re-use Replay message to reset clients
                                Envelope.Broadcast(ServerMessage.ScoresAfterLeaver(data.scores)),
                            ),
                    )
                }

                is GamePhase.InRound -> {
                    // The question chain is broken. Regenerate pairs and restart the round (Round 1.1).
                    val newRoundData = setupRoundSpecifics(data.copy(players = remainingPlayers))
                    val roundStartTransition = onRoundStart(newRoundData) as GameStateTransition.Valid
                    val roundReplayEnvelopes =
                        listOf(
                            Envelope.Broadcast(ServerMessage.PlayerList(remainingPlayers.values.toList())),
                            Envelope.Broadcast(ServerMessage.ReplayRound(incrementRoundNumber = false)),
                            Envelope.Broadcast(ServerMessage.ScoresAfterLeaver(data.scores)),
                        )

                    val replayRoundTransition =
                        roundStartTransition.copy(envelopes = roundReplayEnvelopes + roundStartTransition.envelopes)
                    replayRoundTransition
                }

                is GamePhase.RoundReplayChoice -> {
                    // Just update list, logic holds fine here
                    GameStateTransition.Valid(
                        newGameData = data,
                        envelopes =
                            listOf(
                                Envelope.Broadcast(ServerMessage.PlayerList(remainingPlayers.values.toList())),
                                Envelope.Broadcast(ServerMessage.ScoresAfterLeaver(data.scores)),
                            ),
                    )
                }

                is GamePhase.GameVoting -> {
                    // Remove votes involving the kicked player
                    val cleanVotes =
                        data.votes
                            .filterKeys { it != removedPlayerId } // Remove their vote
                            .filterValues { it != removedPlayerId } // Remove votes FOR them

                    val votingData = data.copy(votes = cleanVotes)

                    GameStateTransition.Valid(
                        newGameData = votingData,
                        envelopes =
                            listOf(
                                Envelope.Broadcast(ServerMessage.PlayerList(remainingPlayers.values.toList())),
                                Envelope.Broadcast(ServerMessage.VotesAfterLeaver(cleanVotes)),
                                Envelope.Broadcast(ServerMessage.ScoresAfterLeaver(data.scores)),
                            ),
                    )
                }

                is GamePhase.ImposterGuess, is GamePhase.GameResults, is GamePhase.GameReplayChoice -> {
                    GameStateTransition.Valid(
                        newGameData = data,
                        envelopes =
                            listOf(
                                Envelope.Broadcast(ServerMessage.PlayerList(remainingPlayers.values.toList())),
                                Envelope.Broadcast(ServerMessage.ScoresAfterLeaver(data.scores)),
                            ),
                    )
                }

                is GamePhase.Idle, GamePhase.Lobby, is GamePhase.Paused, is GamePhase.GameEnd ->
                    GameStateTransition.Invalid(
                        "Cannot kick player when phase is $phase",
                    )
            }
        }
    }
