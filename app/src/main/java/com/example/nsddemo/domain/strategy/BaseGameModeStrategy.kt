package com.example.nsddemo.domain.strategy

import com.example.nsddemo.domain.model.ClientMessage
import com.example.nsddemo.domain.model.Envelope
import com.example.nsddemo.domain.model.GameCategory
import com.example.nsddemo.domain.model.GamePhase
import com.example.nsddemo.domain.model.GameStateTransition
import com.example.nsddemo.domain.model.NewGameData
import com.example.nsddemo.domain.model.ServerMessage
import com.example.nsddemo.domain.repository.WordRepository
import com.example.nsddemo.domain.util.GameFlowRegistry
import com.example.nsddemo.domain.util.GameScoreIncrements
import com.example.nsddemo.domain.util.PlayerCountLimits

abstract class BaseGameModeStrategy(private val wordRepository: WordRepository) : GameModeStrategy {
    override fun handleAction(
        data: NewGameData, phase: GamePhase, message: ClientMessage, playerID: String
    ): GameStateTransition {
        val allowedPhases = GameFlowRegistry.getValidPhasesFor(message)
        if (phase !in allowedPhases) {
            return GameStateTransition.Invalid(
                "Action ${message::class.simpleName} is NOT allowed in phase ${phase::class.simpleName}. " + "Valid phases: ${allowedPhases.joinToString { it::class.simpleName ?: "" }}"
            )
        }

        return when (message) {
            is ClientMessage.RequestSelectCategory -> selectCategory(
                data, message.category, playerID
            )

            is ClientMessage.RequestStartGame -> startGame(
                data, playerID
            )

            ClientMessage.ConfirmRoleReceived -> handleRoleConfirm(
                data, playerID
            )

            ClientMessage.EndTurn -> endTurn(
                data, playerID
            )

            ClientMessage.RequestReplayRound -> replayRound(
                data, playerID
            )

            ClientMessage.RequestStartVote -> startVote(
                data, playerID
            )

            is ClientMessage.SubmitVote -> submitVote(
                data, playerID, message.votedPlayerID
            )

            ClientMessage.RequestContinueToGameChoice -> continueToGameChoice(data, playerID)

            ClientMessage.RequestReplayGame -> replayGame(data, playerID)

            ClientMessage.RequestEndGame -> endGame(data, playerID)

            else -> throw UnsupportedOperationException("Transition for message $message should be handled in GameSessionManager")
        }
    }

    private fun selectCategory(
        data: NewGameData, category: GameCategory, playerID: String
    ): GameStateTransition {
        if (data.hostId != playerID) {
            return GameStateTransition.Invalid("Only host can choose a category")
        }

        val newData = data.copy(category = category)

        return GameStateTransition.Valid(
            newGameData = newData, envelopes = listOf(
                Envelope.Broadcast(
                    ServerMessage.CategorySelected(category)
                )
            )
        )
    }

    private fun startGame(
        data: NewGameData, playerID: String
    ): GameStateTransition {
        if (data.hostId != playerID) {
            return GameStateTransition.Invalid("Only host can start game")
        }

        val selectedCategory =
            data.category ?: return GameStateTransition.Invalid("No category selected")
        if (data.players.size < PlayerCountLimits.MIN_PLAYERS) {
            return GameStateTransition.Invalid("Not enough players")
        }

        val word = wordRepository.getWordsForCategory(selectedCategory).random()
        val imposterId = data.players.keys.random()

        val dataWithRoles = data.copy(
            word = word, imposterId = imposterId
        )
        // Question Mode will generate pairs, Describe Mode will generate order
        val finalData = setupRoundSpecifics(dataWithRoles)

        val messages = mutableListOf<Envelope>()
        finalData.players.keys.forEach { targetPlayerId ->
            val roleMessage = if (targetPlayerId == finalData.imposterId) {
                ServerMessage.RoleAssigned(
                    category = finalData.category!!, word = ""
                )
            } else {
                ServerMessage.RoleAssigned(
                    category = finalData.category!!, word = finalData.word!!
                )
            }

            messages.add(Envelope.Unicast(targetPlayerId, roleMessage))
        }

        return GameStateTransition.Valid(
            newGameData = finalData,
            newPhase = GamePhase.RoleDistribution,
            envelopes = messages
        )
    }

    private fun handleRoleConfirm(
        data: NewGameData, playerId: String
    ): GameStateTransition {
        val dataWithNewConfirmation = data.copy(readyPlayerIds = data.readyPlayerIds + playerId)
        if (dataWithNewConfirmation.readyCount == data.players.size) {
            val cleanData = dataWithNewConfirmation.copy(readyPlayerIds = emptySet())
            return onRoundStart(cleanData)
        }

        return GameStateTransition.Valid(
            newGameData = dataWithNewConfirmation, envelopes = listOf(
                Envelope.Broadcast(
                    ServerMessage.PlayerReady(
                        dataWithNewConfirmation.readyPlayerIds.toList()
                    )
                )
            )
        )
    }

    private fun endTurn(
        data: NewGameData, playerID: String
    ): GameStateTransition {
        return onTurnEnd(data, playerID)
    }

    private fun replayRound(
        data: NewGameData, playerID: String
    ): GameStateTransition {
        if (data.hostId != playerID) {
            return GameStateTransition.Invalid("Only host can replay round")
        }

        val newRoundData = setupRoundSpecifics(data).copy(roundNumber = data.roundNumber + 1)
        val roundStartTransition = onRoundStart(newRoundData) as GameStateTransition.Valid
        val roundReplayMessage = listOf(Envelope.Broadcast(ServerMessage.ReplayRound))

        val replayRoundTransition =
            roundStartTransition.copy(envelopes = roundReplayMessage + roundStartTransition.envelopes)
        return replayRoundTransition
    }

    private fun startVote(
        data: NewGameData, playerID: String
    ): GameStateTransition {
        if (data.hostId != playerID) {
            return GameStateTransition.Invalid("Only host can replay round")
        }

        return GameStateTransition.Valid(
            newGameData = data,
            newPhase = GamePhase.GameVoting,
            envelopes = listOf(Envelope.Broadcast(ServerMessage.StartVote))
        )
    }

    private fun submitVote(
        data: NewGameData, playerID: String, votedPlayerID: String
    ): GameStateTransition {
        if (playerID == votedPlayerID) {
            return GameStateTransition.Invalid("A player can't vote for themselves")
        }
        if (playerID in data.voters) {
            return GameStateTransition.Invalid("A player can only vote once")
        }

        val dataWithVote = data.copy(votes = data.votes + (playerID to votedPlayerID))
        val currentGameScores = calculatePlayerScores(
            dataWithVote.votes, dataWithVote.imposterId!!
        )
        val totalScores = dataWithVote.scores + currentGameScores
        if (dataWithVote.hasEveryoneVoted) {
            return GameStateTransition.Valid(
                newGameData = dataWithVote,
                newPhase = GamePhase.GameResults,
                envelopes = listOf(
                    Envelope.Broadcast(
                        ServerMessage.PlayerVoted(playerID, votedPlayerID)
                    ), Envelope.Broadcast(
                        ServerMessage.VoteResult(
                            voteResult = dataWithVote.votes,
                            imposterId = dataWithVote.imposterId,
                            playerScores = totalScores
                        )
                    )
                )
            )
        }

        return GameStateTransition.Valid(
            newGameData = dataWithVote,
            envelopes = listOf(
                Envelope.Broadcast(
                    ServerMessage.PlayerVoted(
                        playerID,
                        votedPlayerID
                    )
                )
            )
        )
    }

    private fun calculatePlayerScores(
        votes: Map<String, String>, imposterId: String
    ): Map<String, Int> =
        votes.entries.associate { it.key to (if (it.value == imposterId) GameScoreIncrements.CORRECT_PLAYER_GUESS else GameScoreIncrements.INCORRECT_PLAYER_GUESS) }

    private fun continueToGameChoice(data: NewGameData, playerId: String): GameStateTransition {
        if (data.hostId != playerId) return GameStateTransition.Invalid("Only host can continue to game choice")

        return GameStateTransition.Valid(
            newGameData = data, newPhase = GamePhase.GameReplayChoice, envelopes = listOf(
                Envelope.Broadcast(
                    message = ServerMessage.ContinueToGameChoice
                )
            )
        )
    }

    private fun replayGame(
        data: NewGameData, playerID: String
    ): GameStateTransition {
        if (data.hostId != playerID) {
            return GameStateTransition.Invalid("Only host can replay game")
        }

        val cleanData = NewGameData(
            localPlayerId = data.localPlayerId,
            hostId = data.hostId,
            gameCode = data.gameCode,
            players = data.players,
            scores = data.scores
        )

        return GameStateTransition.Valid(
            newGameData = cleanData,
            newPhase = GamePhase.Lobby,
            envelopes = listOf(Envelope.Broadcast(ServerMessage.ReplayGame))
        )
    }

    private fun endGame(
        data: NewGameData, playerId: String
    ): GameStateTransition {
        if (data.hostId != playerId) {
            return GameStateTransition.Invalid("Only host can end game")
        }

        val emptyData = NewGameData()

        return GameStateTransition.Valid(
            newGameData = emptyData,
            newPhase = GamePhase.Idle,
            envelopes = listOf(Envelope.Broadcast(ServerMessage.EndGame))
        )
    }

    //region --- Template Methods ---
    protected abstract fun setupRoundSpecifics(data: NewGameData): NewGameData
    protected abstract fun onRoundStart(data: NewGameData): GameStateTransition
    protected abstract fun onTurnEnd(data: NewGameData, playerID: String): GameStateTransition
    //endregion
}