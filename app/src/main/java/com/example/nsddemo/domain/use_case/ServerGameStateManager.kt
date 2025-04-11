package com.example.nsddemo.domain.use_case

import android.util.Log
import com.example.nsddemo.core.util.Debugging.TAG
import com.example.nsddemo.core.util.GameState
import com.example.nsddemo.data.local.network.socket.Server
import com.example.nsddemo.data.repository.GameRepository
import com.example.nsddemo.data.util.KtorSocketUtil.sendUtf8LineToAllPlayers
import com.example.nsddemo.data.util.KtorSocketUtil.writeLineUtf8
import com.example.nsddemo.domain.model.Player
import com.example.nsddemo.domain.util.generateRoundPlayerPairs
import com.example.nsddemo.presentation.util.PlayerColors
import com.google.gson.Gson
import com.google.gson.GsonBuilder

class ServerGameStateManager(
    private val gameRepository: GameRepository, private val gson: Gson
) {
    suspend fun handleReplayState() {
        val currentGameState = gameRepository.gameState.value as GameState.Replay
        Log.d(TAG, "Sending replay status to all players.")
        sendUtf8LineToAllPlayers {
            gson.toJson(currentGameState.replay)
        }
        gameRepository.updateGameData(gameRepository.gameData.value.copy(isFirstRound = false))
    }

    fun handleShowScoreboardState() {

    }

    suspend fun handleEndVoteState() {
        val complexGson = GsonBuilder().enableComplexMapKeySerialization().create()
        val updatedPlayerScores = gameRepository.gameData.value.playerScores.toMutableMap()
        for ((voter, voted) in gameRepository.gameData.value.roundPlayerVotes) {
            val scoreIncrement =
                if (voted == gameRepository.gameData.value.imposter) PLAYER_SCORE_INCREMENT else 0
            updatedPlayerScores[voter] = (updatedPlayerScores[voter] ?: 0) + scoreIncrement
        }
        gameRepository.updateGameData(
            gameRepository.gameData.value.copy(
                playerScores = updatedPlayerScores
            )
        )
        sendUtf8LineToAllPlayers {
            gson.toJson(gameRepository.gameData.value.imposter)
        }
        sendUtf8LineToAllPlayers {
            complexGson.toJson(gameRepository.gameData.value.roundVotingCounts)
        }
        sendUtf8LineToAllPlayers {
            complexGson.toJson(gameRepository.gameData.value.playerScores)
        }
    }

    fun handleGetPlayerVoteState() {
        val currentGameState = gameRepository.gameState.value as GameState.GetPlayerVote
        val votingPlayer = currentGameState.voter
        val votedPlayer = currentGameState.voted
        Log.d(
            TAG, "$votingPlayer voted for $votedPlayer"
        )
        val updatedRoundPlayerVotes =
            gameRepository.gameData.value.roundPlayerVotes.toMutableMap().also {
                it[votingPlayer] = votedPlayer
            }
        val updatedRoundVotingCounts =
            gameRepository.gameData.value.roundVotingCounts.toMutableMap().also {
                it[votedPlayer] = (it[votedPlayer] ?: 0) + 1
            }
        gameRepository.updateGameData(
            gameRepository.gameData.value.copy(
                roundPlayerVotes = updatedRoundPlayerVotes,
                roundVotingCounts = updatedRoundVotingCounts,
            )
        )
        val numberOfPlayersWhoVoted = gameRepository.gameData.value.numberOfPlayersWhoVoted
        val totalNumberOfPlayers = gameRepository.gameData.value.players.size
        Log.d(
            TAG,
            "numberOfPlayersWhoVoted=$numberOfPlayersWhoVoted, totalNumberOfPlayers=$totalNumberOfPlayers"
        )
        if (numberOfPlayersWhoVoted != totalNumberOfPlayers) {
            return
        }
        Log.d(TAG, "All players voted. Ending vote...")
        val playerWithHighestVotes =
            gameRepository.gameData.value.roundVotingCounts.maxBy { it.value }.key
        gameRepository.updateGameState(GameState.EndVote(playerWithHighestVotes))
    }

    fun handleGetCurrentPlayerVoteState() {
        val currentGameState = gameRepository.gameState.value as GameState.GetCurrentPlayerVote
        val gameData = gameRepository.gameData.value
        gameRepository.updateGameState(
            GameState.GetPlayerVote(
                gameData.currentPlayer!!, currentGameState.voted
            )
        )
    }

    suspend fun handleStartVoteState() {
        // This is sent to exit "additional questions" loop on client side
        Log.d(
            TAG, "Start Vote: Sending 'additional questions' FALSE message to all players..."
        )
        sendUtf8LineToAllPlayers {
            gson.toJson(false)
        }
        Log.d(
            TAG, "Start Vote: Sending start vote flag to each player..."
        )
        sendUtf8LineToAllPlayers {
            gson.toJson(true)
        }
    }

    fun handleAskExtraQuestionsState() {
        Log.d(
            TAG,
            "AskExtraQuestions state: resetting round player pairs & starting to ask questions..."
        )
        gameRepository.updateGameData(
            gameRepository.gameData.value.copy(
                roundPlayerPairs = generateRoundPlayerPairs(gameRepository.gameData.value.players),
                currentPlayerPairIndex = 0
            )
        )
        val gameData = gameRepository.gameData.value
        val (askingPlayer, askedPlayer) = gameData.currentPlayerPair
        val isAsking = gameData.isAsking
        val isLastQuestion = gameData.isLastQuestion
        gameRepository.updateGameState(
            GameState.AskQuestion(
                askingPlayer, askedPlayer, isAsking, isLastQuestion, true
            )
        )
    }

    suspend fun handleChooseExtraQuestionsState() {
        Log.d(TAG, "ChooseExtraQuestions state: Sending TRUE message to clients")
        sendUtf8LineToAllPlayers {
            gson.toJson(true)
        }
    }

    fun handleConfirmCurrentPlayerQuestion() {
        // TODO: this and AskQuestion state in updateStateOnMessageReceivedServerSide should be merged
        val currentGameState =
            gameRepository.gameState.value as GameState.ConfirmCurrentPlayerQuestion
        val currentAskQuestionState = currentGameState.currentAskQuestionState
        if (currentAskQuestionState.isLastQuestion) {
            // Handles if the last question was asked by the host
            Log.d(TAG, "Choosing to either ask extra questions or start vote...")
            gameRepository.updateGameState(GameState.ChooseExtraQuestions)
        } else {
            Log.d(TAG, "Asking another question...")
            val gameData = gameRepository.gameData.value
            val (askingPlayer, askedPlayer) = gameData.currentPlayerPair
            val isAsking = gameData.isAsking
            val isLastQuestion = gameData.isLastQuestion
            gameRepository.updateGameState(
                GameState.AskQuestion(
                    askingPlayer, askedPlayer, isAsking, isLastQuestion
                )
            )
        }
    }

    suspend fun handleAskQuestionState() {
        val currentGameState = gameRepository.gameState.value as GameState.AskQuestion
        if (currentGameState.isFirstQuestionInNewRound) {
            Log.d(
                TAG, "Sending 'additional questions' TRUE message to all players..."
            )
            sendUtf8LineToAllPlayers {
                gson.toJson(true)
            }
        }
        Log.d(TAG, "Sending current question to all players")
        sendUtf8LineToAllPlayers { player ->
            gson.toJson(
                currentGameState.copy(
                    isAsking = currentGameState.asker == player,
                )
            )
        }
        gameRepository.updateGameData(gameRepository.gameData.value.copy(currentPlayerPairIndex = gameRepository.gameData.value.currentPlayerPairIndex + 1))
    }

    fun handleGetPlayerReadCategoryAndWordConfirmationState() {
        val currentGameState =
            gameRepository.gameState.value as GameState.GetPlayerReadCategoryAndWordConfirmation
        val numberOfReadCategoryAndWordConfirmations: Int = currentGameState.numberOfConfirmations
        Log.d(TAG, "# of Confirmations= $numberOfReadCategoryAndWordConfirmations")
        val numberOfPlayers = gameRepository.gameData.value.players.size
        if (numberOfReadCategoryAndWordConfirmations != numberOfPlayers) {
            return
        }
        Log.d(
            TAG, "# of Confirmations = $numberOfReadCategoryAndWordConfirmations"
        )
        Log.d(TAG, "All players have read the category and word.")
        // Ask first question
        val (askingPlayer, askedPlayer) = gameRepository.gameData.value.currentPlayerPair
        val isAsking = gameRepository.gameData.value.isAsking
        val isLastQuestion = gameRepository.gameData.value.isLastQuestion
        gameRepository.updateGameState(
            GameState.AskQuestion(
                askingPlayer,
                askedPlayer,
                isAsking,
                isLastQuestion,
            )
        )
    }

    fun handleConfirmCurrentPlayerCategoryAndWord() {
        val currentGameState =
            gameRepository.gameState.value as GameState.ConfirmCurrentPlayerReadCategoryAndWord
        gameRepository.updateGameState(
            GameState.GetPlayerReadCategoryAndWordConfirmation(
                currentGameState.numberOfConfirmations
            )
        )
    }

    // TODO: Sometimes this is called twice somehow to be fixed later
    //  Generally, this is caused by two consecutive screens adding the same state in their handler
    //  causing duplicate handling
    //  Solution: Maybe centralize handling again but in a cleaner way
    suspend fun handleDisplayCategoryAndWordState() {
        // Send a message to players indicating that the lastPlayer has joined
        // and the game is ready to start
        sendUtf8LineToAllPlayers { gson.toJson(true) }
        val playersIncludingServer = Server.clients.values.toMutableList().also {
            it.add(gameRepository.gameData.value.currentPlayer!!)
        }.toList()
        val imposter = playersIncludingServer.random()
        Log.d(TAG, "Players including server: $playersIncludingServer")
        val roundPlayerPairs = generateRoundPlayerPairs(playersIncludingServer)
        val currentGameState = gameRepository.gameState.value as GameState.DisplayCategoryAndWord
        val categoryOrdinal = currentGameState.categoryOrdinal
        val wordResID = currentGameState.wordResourceId
        gameRepository.updateGameData(
            gameRepository.gameData.value.copy(
                imposter = imposter,
                roundPlayerPairs = roundPlayerPairs,
                categoryOrdinal = categoryOrdinal,
                wordResID = wordResID
            )
        )
        Log.d(TAG, "Imposter: ${gameRepository.gameData.value.imposter}")
        Log.d(TAG, "Sending category and word to all players")
        sendUtf8LineToAllPlayers { player ->
            gson.toJson(
                if (player == gameRepository.gameData.value.imposter) mapOf("category" to gameRepository.gameData.value.categoryOrdinal)
                else mapOf(
                    "category" to gameRepository.gameData.value.categoryOrdinal,
                    "word" to gameRepository.gameData.value.wordResID
                )
            )
        }
    }

    suspend fun handleGetPlayerInfoState() {
        val currentGameState = gameRepository.gameState.value as GameState.GetPlayerInfo
        Log.d(TAG, "currentPlayer = ${gameRepository.gameData.value.currentPlayer}")
        Log.d(
            TAG, "selectedPlayerColors: ${gameRepository.gameData.value.selectedPlayerColors}"
        )
        val newPlayerColor = PlayerColors.entries
            .filter { it !in gameRepository.gameData.value.selectedPlayerColors }.random()
        val newPlayer = Player(currentGameState.name, newPlayerColor.argb.toString())
        val updatedPlayers = gameRepository.gameData.value.players.toMutableList().also {
            it.add(newPlayer)
        }
        gameRepository.updateGameData(gameRepository.gameData.value.copy(players = updatedPlayers))

        val newPlayerConnection = currentGameState.connection
        Server.clients[newPlayerConnection] = newPlayer

        newPlayerConnection.output.writeLineUtf8(
            gson.toJson(newPlayerColor.argb.toString())
        )

        // This is used to tell the client that a player has joined the game and it's NOT the
        // last player as in the host hasn't clicked start game yet
        sendUtf8LineToAllPlayers { gson.toJson(false) }

        sendUtf8LineToAllPlayers {
            gson.toJson(gameRepository.gameData.value.players)
        }
    }

    fun handleStartGameState() {
        val gameData = gameRepository.gameData.value
        if (!gameData.isFirstRound) return
        val currentPlayerColor =
            PlayerColors.entries.filter { it !in gameData.selectedPlayerColors }.random()
        val currentPlayer = Player(
            gameData.currentPlayer!!.name,
            currentPlayerColor.argb.toString(),
        )
        val updatedPlayers = gameData.players.toMutableList().also {
            it.add(currentPlayer)
        }
        gameRepository.updateGameData(
            gameData.copy(
                currentPlayer = currentPlayer, players = updatedPlayers
            )
        )
        Log.d(
            TAG, "Current player: ${gameData.currentPlayer}"
        )
    }
    // endregion

    companion object {
        const val PLAYER_SCORE_INCREMENT = 100
    }
}