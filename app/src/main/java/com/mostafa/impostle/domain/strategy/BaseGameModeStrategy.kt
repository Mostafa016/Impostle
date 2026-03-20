package com.mostafa.impostle.domain.strategy

import android.util.Log
import com.mostafa.impostle.core.util.Debugging.TAG
import com.mostafa.impostle.domain.model.ClientMessage
import com.mostafa.impostle.domain.model.Envelope
import com.mostafa.impostle.domain.model.GameCategory
import com.mostafa.impostle.domain.model.GameData
import com.mostafa.impostle.domain.model.GamePhase
import com.mostafa.impostle.domain.model.GameStateTransition
import com.mostafa.impostle.domain.model.ServerMessage
import com.mostafa.impostle.domain.repository.WordRepository
import com.mostafa.impostle.domain.util.GameFlowRegistry
import com.mostafa.impostle.domain.util.GameScoreIncrements
import com.mostafa.impostle.domain.util.PlayerCountLimits

abstract class BaseGameModeStrategy(
    private val wordRepository: WordRepository,
) : GameModeStrategy {
    override fun handleAction(
        data: GameData,
        phase: GamePhase,
        message: ClientMessage,
        playerID: String,
    ): GameStateTransition {
        val allowedPhases = GameFlowRegistry.getValidPhasesFor(message)
        if (phase !in allowedPhases) {
            return GameStateTransition.Invalid(
                "Action ${message::class.simpleName} is NOT allowed in phase ${phase::class.simpleName}. " +
                    "Valid phases: ${allowedPhases.joinToString { it::class.simpleName ?: "" }}",
            )
        }

        return when (message) {
            is ClientMessage.RequestSelectCategory ->
                selectCategory(
                    data,
                    message.category,
                    playerID,
                )

            is ClientMessage.RequestStartGame ->
                startGame(
                    data,
                    playerID,
                )

            is ClientMessage.ConfirmRoleReceived ->
                handleRoleConfirm(
                    data,
                    playerID,
                )

            ClientMessage.EndTurn ->
                endTurn(
                    data,
                    playerID,
                )

            ClientMessage.RequestReplayRound ->
                replayRound(
                    data,
                    playerID,
                )

            ClientMessage.RequestStartVote ->
                startVote(
                    data,
                    playerID,
                )

            is ClientMessage.SubmitVote ->
                submitVote(
                    data,
                    playerID,
                    message.votedPlayerID,
                )

            is ClientMessage.SubmitImposterGuess ->
                submitImposterGuess(
                    data,
                    playerID,
                    message.guessedWord,
                )

            ClientMessage.RequestContinueToGameChoice -> continueToGameChoice(data, playerID)

            ClientMessage.RequestReplayGame -> replayGame(data, playerID)

            ClientMessage.RequestEndGame -> endGame(data, playerID)

            else -> throw UnsupportedOperationException("Transition for message $message should be handled in GameSessionManager")
        }
    }

    private fun selectCategory(
        data: GameData,
        category: GameCategory,
        playerID: String,
    ): GameStateTransition {
        if (data.hostId != playerID) {
            Log.i(TAG, "hostId: ${data.hostId} playerID: $playerID")
            return GameStateTransition.Invalid("Only host can choose a category")
        }

        val newData = data.copy(category = category)

        return GameStateTransition.Valid(
            newGameData = newData,
            envelopes =
                listOf(
                    Envelope.Broadcast(
                        ServerMessage.CategorySelected(category),
                    ),
                ),
        )
    }

    private fun startGame(
        data: GameData,
        playerID: String,
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
        val imposterId =
            data.players.keys
                .random()

        val dataWithRoles =
            data.copy(
                word = word,
                imposterId = imposterId,
            )
        // Question Mode will generate pairs, Describe Mode will generate order
        val finalData = setupRoundSpecifics(dataWithRoles)

        val messages = mutableListOf<Envelope>()
        finalData.players.keys.forEach { targetPlayerId ->
            val roleMessage =
                if (targetPlayerId == finalData.imposterId) {
                    ServerMessage.RoleAssigned(
                        category = finalData.category!!,
                        word = "",
                    )
                } else {
                    ServerMessage.RoleAssigned(
                        category = finalData.category!!,
                        word = finalData.word!!,
                    )
                }

            messages.add(Envelope.Unicast(targetPlayerId, roleMessage))
        }

        return GameStateTransition.Valid(
            newGameData = finalData,
            newPhase = GamePhase.RoleDistribution,
            envelopes = messages,
        )
    }

    private fun handleRoleConfirm(
        data: GameData,
        playerId: String,
    ): GameStateTransition {
        val dataWithNewConfirmation = data.copy(readyPlayerIds = data.readyPlayerIds + playerId)
        if (dataWithNewConfirmation.readyCount == data.players.size) {
            val cleanData = dataWithNewConfirmation.copy(readyPlayerIds = emptySet())
            return onRoundStart(cleanData)
        }

        return GameStateTransition.Valid(
            newGameData = dataWithNewConfirmation,
            envelopes =
                listOf(
                    Envelope.Broadcast(
                        ServerMessage.PlayerReady(
                            dataWithNewConfirmation.readyPlayerIds.toList(),
                        ),
                    ),
                ),
        )
    }

    private fun endTurn(
        data: GameData,
        playerID: String,
    ): GameStateTransition = onTurnEnd(data, playerID)

    private fun replayRound(
        data: GameData,
        playerID: String,
    ): GameStateTransition {
        if (data.hostId != playerID) {
            return GameStateTransition.Invalid("Only host can replay round")
        }

        val newRoundData = setupRoundSpecifics(data).copy(roundNumber = data.roundNumber + 1)
        val roundStartTransition = onRoundStart(newRoundData) as GameStateTransition.Valid
        val roundReplayMessage = listOf(Envelope.Broadcast(ServerMessage.ReplayRound()))

        val replayRoundTransition =
            roundStartTransition.copy(envelopes = roundReplayMessage + roundStartTransition.envelopes)
        return replayRoundTransition
    }

    private fun startVote(
        data: GameData,
        playerID: String,
    ): GameStateTransition {
        if (data.hostId != playerID) {
            return GameStateTransition.Invalid("Only host can replay round")
        }

        return GameStateTransition.Valid(
            newGameData = data,
            newPhase = GamePhase.GameVoting,
            envelopes = listOf(Envelope.Broadcast(ServerMessage.StartVote)),
        )
    }

    private fun submitVote(
        data: GameData,
        playerID: String,
        votedPlayerID: String,
    ): GameStateTransition {
        if (playerID == votedPlayerID) {
            return GameStateTransition.Invalid("A player can't vote for themselves")
        }
        if (playerID in data.voters) {
            return GameStateTransition.Invalid("A player can only vote once")
        }

        val dataWithVote = data.copy(votes = data.votes + (playerID to votedPlayerID))
        if (dataWithVote.hasEveryoneVoted) {
            val currentGameScores =
                calculatePlayerScores(
                    dataWithVote.votes,
                    dataWithVote.imposterId!!,
                )
            val totalScores =
                (dataWithVote.scores.toList() + currentGameScores.toList())
                    .groupBy({ it.first }, { it.second })
                    .map { (key, values) -> key to values.sum() }
                    .toMap()
            val dataWithScores = dataWithVote.copy(scores = totalScores)

            val actualWord =
                dataWithScores.word ?: return GameStateTransition.Invalid("No word set")
            val category =
                dataWithScores.category ?: return GameStateTransition.Invalid("No category set")

            // 1 actual word, 3 semantic, 2 random
            val allWords = wordRepository.getWordsForCategory(category)
            val semantics =
                wordRepository
                    .getSemanticWords(actualWord)
                    .filter { it != actualWord }
                    .shuffled()
                    .take(3)

            val neededFromSemantic = 3
            val actualSemantics =
                if (semantics.size < neededFromSemantic) {
                    val neededMore = neededFromSemantic - semantics.size
                    val candidates =
                        allWords.filter { it != actualWord && it !in semantics }.shuffled()
                    semantics + candidates.take(neededMore)
                } else {
                    semantics
                }

            val neededRandom = 2
            val randoms =
                allWords
                    .filter { it != actualWord && it !in actualSemantics }
                    .shuffled()
                    .take(neededRandom)

            val options = (listOf(actualWord) + actualSemantics + randoms).shuffled()

            val finalData = dataWithScores.copy(wordOptions = options)

            return GameStateTransition.Valid(
                newGameData = finalData,
                newPhase = GamePhase.ImposterGuess,
                envelopes =
                    listOf(
                        Envelope.Broadcast(
                            ServerMessage.PlayerVoted(playerID, votedPlayerID),
                        ),
                        Envelope.Broadcast(
                            ServerMessage.StartImposterGuess(options),
                        ),
                    ),
            )
        }

        return GameStateTransition.Valid(
            newGameData = dataWithVote,
            envelopes =
                listOf(
                    Envelope.Broadcast(
                        ServerMessage.PlayerVoted(
                            playerID,
                            votedPlayerID,
                        ),
                    ),
                ),
        )
    }

    private fun submitImposterGuess(
        data: GameData,
        playerID: String,
        guessedWord: String,
    ): GameStateTransition {
        if (data.imposterId != playerID) {
            return GameStateTransition.Invalid("Only the imposter can submit a guess")
        }

        val isCorrect = guessedWord == data.word
        val increment =
            if (isCorrect) GameScoreIncrements.CORRECT_IMPOSTER_GUESS else GameScoreIncrements.INCORRECT_IMPOSTER_GUESS

        val updatedScores = data.scores.toMutableMap()
        updatedScores[playerID] = (updatedScores[playerID] ?: 0) + increment

        val finalData = data.copy(scores = updatedScores)

        return GameStateTransition.Valid(
            newGameData = finalData,
            newPhase = GamePhase.GameResults,
            envelopes =
                listOf(
                    Envelope.Broadcast(
                        ServerMessage.VoteResult(
                            voteResult = finalData.votes,
                            imposterId = finalData.imposterId!!,
                            playerScores = finalData.scores,
                        ),
                    ),
                ),
        )
    }

    private fun calculatePlayerScores(
        votes: Map<String, String>,
        imposterId: String,
    ): Map<String, Int> =
        votes.entries.associate {
            it.key to
                (if (it.value == imposterId) GameScoreIncrements.CORRECT_PLAYER_GUESS else GameScoreIncrements.INCORRECT_PLAYER_GUESS)
        }

    private fun continueToGameChoice(
        data: GameData,
        playerId: String,
    ): GameStateTransition {
        if (data.hostId != playerId) return GameStateTransition.Invalid("Only host can continue to game choice")

        return GameStateTransition.Valid(
            newGameData = data,
            newPhase = GamePhase.GameReplayChoice,
            envelopes =
                listOf(
                    Envelope.Broadcast(
                        message = ServerMessage.ContinueToGameChoice,
                    ),
                ),
        )
    }

    private fun replayGame(
        data: GameData,
        playerID: String,
    ): GameStateTransition {
        if (data.hostId != playerID) {
            return GameStateTransition.Invalid("Only host can replay game")
        }

        val cleanData =
            GameData(
                localPlayerId = data.localPlayerId,
                hostId = data.hostId,
                gameCode = data.gameCode,
                players = data.players.filterValues { it.isConnected },
                scores = data.scores,
            )

        return GameStateTransition.Valid(
            newGameData = cleanData,
            newPhase = GamePhase.Lobby,
            envelopes =
                listOf(
                    Envelope.Broadcast(ServerMessage.ReplayGame),
                    Envelope.Broadcast(ServerMessage.PlayerList(cleanData.players.values.toList())),
                ),
        )
    }

    private fun endGame(
        data: GameData,
        playerId: String,
    ): GameStateTransition {
        if (data.hostId != playerId) {
            return GameStateTransition.Invalid("Only host can end game")
        }

        val emptyData = GameData()

        return GameStateTransition.Valid(
            newGameData = emptyData,
            newPhase = GamePhase.Idle,
            envelopes = listOf(Envelope.Broadcast(ServerMessage.EndGame)),
        )
    }

    //region --- Template Methods ---
    protected abstract fun setupRoundSpecifics(data: GameData): GameData

    protected abstract fun onRoundStart(data: GameData): GameStateTransition

    protected abstract fun onTurnEnd(
        data: GameData,
        playerID: String,
    ): GameStateTransition
    //endregion
}
