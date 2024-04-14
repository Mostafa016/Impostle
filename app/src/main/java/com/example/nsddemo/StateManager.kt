package com.example.nsddemo

import android.util.Log
import com.example.nsddemo.network.Server
import com.example.nsddemo.ui.PlayerColors
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.writeStringUtf8
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class StateManager(
    private val gameManager: GameManager,
    private val gameRepository: GameRepository,
    private val gson: Gson

) {
    // region GameState handling
    fun reactToGameStateChanges(externalScope: CoroutineScope): Job {
        // https://developer.android.com/kotlin/coroutines/coroutines-best-practices#:~:text=If%20the%20coroutine%20needs%20to,Internet%20or%20formatting%20a%20String.
        return externalScope.launch(Dispatchers.IO) {
            gameRepository.gameState.collect {
                Log.d(Debugging.TAG, "**********GameState changed to $it**********")
                when (val currentGameState = gameRepository.gameState.value) {
                    GameState.StartGame -> handleStartGameState()

                    is GameState.GetPlayerInfo -> handleGetPlayerInfoState()

                    // This game state will happen when the user which acts as the server
                    // presses a button like "start game"
                    is GameState.DisplayCategoryAndWord -> handleDisplayCategoryAndWordState()

                    is GameState.GetPlayerReadCategoryAndWordConfirmation -> handleGetPlayerReadCategoryAndWordConfirmationState()

                    is GameState.AskQuestion -> handleAskQuestionState()

                    GameState.ChooseExtraQuestions -> handleChooseExtraQuestionsState()

                    GameState.AskExtraQuestions -> handleAskExtraQuestionsState()

                    // This game state will happen when the user which acts as the server
                    // presses a button like "start vote"
                    // (The screen will be like any additional questions?)
                    GameState.StartVote -> handleStartVoteState()

                    is GameState.GetPlayerVote -> handleGetPlayerVoteState()

                    //This event is triggered when all players have voted
                    // Should send the votes list to all players
                    is GameState.EndVote -> handleEndVoteState()

                    // Triggered by user pressing a button (Should probably be sent with the votes list to
                    // be ready to be shown on screen right after to all players
                    GameState.ShowScoreboard -> handleShowScoreboardState()

                    // - For now this should send a message to all clients informing them
                    // that the game will be continued.
                    // - For now also this will be determined by the server, no voting.
                    is GameState.Replay -> handleReplayState()
                }
            }
        }
    }

    private suspend fun handleReplayState() {
        val currentGameState = gameRepository.gameState.value as GameState.Replay
        Log.d(Debugging.TAG, "Sending replay status to all players.")
        sendUtf8LineToAllPlayers {
            gson.toJson(currentGameState.replay)
        }
        gameRepository.updateGameData(gameRepository.gameData.value.copy(isFirstRound = false))
    }

    private fun handleShowScoreboardState() {

    }

    private suspend fun handleEndVoteState() {
        val complexGson = GsonBuilder().enableComplexMapKeySerialization().create()
        val updatedPlayerScores = gameRepository.gameData.value.playerScores.toMutableMap()
        for ((voter, voted) in gameRepository.gameData.value.roundPlayerVotes) {
            updatedPlayerScores[voter] = (updatedPlayerScores[voter]
                ?: 0) + if (voted == gameRepository.gameData.value.imposter) PLAYER_SCORE_INCREMENT else 0
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

    private fun handleGetPlayerVoteState() {
        val currentGameState = gameRepository.gameState.value as GameState.GetPlayerVote
        val votingPlayer = currentGameState.voter
        val votedPlayer = currentGameState.voted
        Log.d(
            Debugging.TAG, "$votingPlayer voted for $votedPlayer"
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
            Debugging.TAG,
            "numberOfPlayersWhoVoted=$numberOfPlayersWhoVoted, totalNumberOfPlayers=$totalNumberOfPlayers"
        )
        if (numberOfPlayersWhoVoted != totalNumberOfPlayers) {
            return
        }
        Log.d(Debugging.TAG, "All players voted. Ending vote...")
        val playerWithHighestVotes =
            gameRepository.gameData.value.roundVotingCounts.maxBy { it.value }.key
        gameRepository.updateGameState(GameState.EndVote(playerWithHighestVotes))
    }

    private suspend fun handleStartVoteState() {
        // This is sent to exit "additional questions" loop on client side
        Log.d(
            Debugging.TAG,
            "Start Vote: Sending 'additional questions' FALSE message to all players..."
        )
        sendUtf8LineToAllPlayers {
            gson.toJson(false)
        }
        Log.d(
            Debugging.TAG, "Start Vote: Sending start vote flag to each player..."
        )
        sendUtf8LineToAllPlayers {
            gson.toJson(true)
        }
    }

    private fun handleAskExtraQuestionsState() {
        gameRepository.updateGameData(
            gameRepository.gameData.value.copy(
                roundPlayerPairs = generateAllAskingCombinations(gameRepository.gameData.value.players),
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

    private suspend fun handleChooseExtraQuestionsState() {
        Log.d(Debugging.TAG, "ChooseExtraQuestions state: Sending TRUE message to clients")
        sendUtf8LineToAllPlayers {
            gson.toJson(true)
        }
    }

    private suspend fun handleAskQuestionState() {
        val currentGameState = gameRepository.gameState.value as GameState.AskQuestion
        if (currentGameState.isFirstQuestionInNewRound) {
            Log.d(
                Debugging.TAG, "Sending 'additional questions' TRUE message to all players..."
            )
            sendUtf8LineToAllPlayers {
                gson.toJson(true)
            }
        }
        sendUtf8LineToAllPlayers { player ->
            gson.toJson(
                currentGameState.copy(
                    isAsking = currentGameState.asker == player
                )
            )
        }
        gameRepository.updateGameData(gameRepository.gameData.value.copy(currentPlayerPairIndex = gameRepository.gameData.value.currentPlayerPairIndex + 1))
    }

    private fun handleGetPlayerReadCategoryAndWordConfirmationState() {
        val currentGameState =
            gameRepository.gameState.value as GameState.GetPlayerReadCategoryAndWordConfirmation
        val numberOfReadCategoryAndWordConfirmations: Int = currentGameState.numberOfConfirmations
        Log.d(Debugging.TAG, "# of Confirmations= $numberOfReadCategoryAndWordConfirmations")
        val numberOfPlayers = gameRepository.gameData.value.players.size
        Log.d(Debugging.TAG, "gameData.players.size= $numberOfPlayers")
        if (numberOfReadCategoryAndWordConfirmations == numberOfPlayers) {
            Log.d(
                Debugging.TAG, "# of Confirmations = $numberOfReadCategoryAndWordConfirmations"
            )
            Log.d(Debugging.TAG, "All players have read the category and word.")
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
    }

    private suspend fun handleDisplayCategoryAndWordState() {
        // Send a message to players indicating that the lastPlayer has joined
        // and the game is ready to start
        sendUtf8LineToAllPlayers { gson.toJson(true) }
        val playersIncludingServer = Server.clients.values.toMutableList().also {
            it.add(gameRepository.gameData.value.currentPlayer!!)
        }
        val imposter = playersIncludingServer.random()
        val roundPlayerPairs = generateAllAskingCombinations(playersIncludingServer)
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
        Log.d(Debugging.TAG, "Imposter: ${gameRepository.gameData.value.imposter}")
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

    private suspend fun handleGetPlayerInfoState() {
        val currentGameState = gameRepository.gameState.value as GameState.GetPlayerInfo
        Log.d(Debugging.TAG, "currentPlayer = ${gameRepository.gameData.value.currentPlayer}")
        Log.d(
            Debugging.TAG,
            "selectedPlayerColors: ${gameRepository.gameData.value.selectedPlayerColors}"
        )
        val newPlayerColor = PlayerColors.values()
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

    private fun handleStartGameState() {
        if (!gameRepository.gameData.value.isFirstRound) return
        val currentPlayerColor = PlayerColors.values()
            .filter { it !in gameRepository.gameData.value.selectedPlayerColors }.random()
        val currentPlayer = Player(
            gameRepository.gameData.value.currentPlayer!!.name,
            currentPlayerColor.argb.toString(),
        )
        val updatedPlayers = gameRepository.gameData.value.players.toMutableList().also {
            it.add(currentPlayer)
        }
        gameRepository.updateGameData(
            gameRepository.gameData.value.copy(
                currentPlayer = currentPlayer, players = updatedPlayers
            )
        )
        Log.d(
            Debugging.TAG, "Current player: ${gameRepository.gameData.value.currentPlayer}"
        )
    }

    // endregion
    fun askNextQuestionOrGoToExtraQuestionsChoice() {
        // TODO: this and AskQuestion state in updateStateOnMessageReceivedServerSide should be merged
        val currentGameState = gameRepository.gameState.value as GameState.AskQuestion
        if (currentGameState.isLastQuestion) {
            // Handles if the last question was asked by the host
            Log.d(Debugging.TAG, "Choosing to either extra questions or start vote...")
            gameRepository.updateGameState(GameState.ChooseExtraQuestions)
        } else {
            Log.d(Debugging.TAG, "Asking another question...")
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

    fun startNewQuestionsRound() {
        gameRepository.updateGameState(GameState.AskExtraQuestions)
    }

    fun confirmReadCategoryAndWord() {
        when (val currentGameState = gameRepository.gameState.value) {
            is GameState.DisplayCategoryAndWord -> {
                gameRepository.updateGameState(GameState.GetPlayerReadCategoryAndWordConfirmation(1))
            }

            is GameState.GetPlayerReadCategoryAndWordConfirmation -> {
                gameRepository.updateGameState(
                    GameState.GetPlayerReadCategoryAndWordConfirmation(
                        currentGameState.numberOfConfirmations + 1
                    )
                )
            }

            else -> {
                Log.wtf(Debugging.TAG, "onConfirmClick called in an invalid state.")
            }
        }
    }

    // region General utility functions (To be moved to GameManager)
    private fun generateAllAskingCombinations(askingPlayers: List<Player>): List<Pair<Player, Player>> {
        val askedPlayers = askingPlayers.toMutableList()
        val chosenAskingPlayers = mutableListOf<Player>()
        return askingPlayers.shuffled().mapIndexed { i, askingPlayer ->
            chosenAskingPlayers.add(askingPlayer)
            val askingPlayerIndex = askedPlayers.indexOf(askingPlayer)
            val askedPlayer = if (i == askingPlayers.lastIndex - 1) {
                val commonPlayers = askedPlayers.filter { it !in chosenAskingPlayers }
                if (commonPlayers.size == 1) {
                    // To handle the following case: A B C D
                    // Wrong Choice: D to B, B to A, {A to D}, C to C
                    // Correct Choice: D to B, B to A, {A to C}, C to D
                    // {} means indicates the pair choice that can lead to an incorrect last pair
                    askedPlayers.find { it == commonPlayers.first() }!!
                } else {
                    askedPlayers.find { it != askingPlayer }!!
                }
            } else {
                askedPlayers.filterIndexed { j, _ -> j != askingPlayerIndex }.random()
            }
            askedPlayers.remove(askedPlayer)
            return@mapIndexed (askingPlayer to askedPlayer)
        }
    }

    // endregion

    // region Message utility functions (To be moved to MessagingService or similar class (MessageHelper))
    private suspend fun sendUtf8LineToAllPlayers(messageFun: (Player) -> String) {
        for ((clientConnection, player) in Server.clients) {
            clientConnection.output.writeLineUtf8(messageFun(player))
        }
    }

    private suspend fun ByteWriteChannel.writeLineUtf8(string: String) =
        writeStringUtf8(string.appendNewLine())

    private fun String.appendNewLine(): String {
        return this + '\n'
    }

    // endregion
    companion object {
        const val PLAYER_SCORE_INCREMENT = 100
    }

}