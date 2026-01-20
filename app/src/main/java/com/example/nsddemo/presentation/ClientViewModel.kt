package com.example.nsddemo.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.nsddemo.core.util.Debugging
import com.example.nsddemo.core.util.GameState
import com.example.nsddemo.data.local.network.NSDHelper
import com.example.nsddemo.data.local.network.socket.Client
import com.example.nsddemo.data.repository.GameRepository
import com.example.nsddemo.data.util.KtorSocketUtil.writeLineUtf8
import com.example.nsddemo.domain.legacy.GameData
import com.example.nsddemo.domain.model.Categories
import com.example.nsddemo.domain.model.Player
import com.example.nsddemo.domain.repository.ClientNetworkRepository
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import io.ktor.network.sockets.Connection
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ClientViewModel(
    private val gameRepository: GameRepository,
    private val nsdHelper: NSDHelper,
    private val clientNetworkRepository: ClientNetworkRepository
) : ViewModel() {
    // TODO: The logic for creating and managing the client should be moved to a repository
    //  to support online multiplayer games in the future
    private val gameState = gameRepository.gameState
    private val gameData = gameRepository.gameData

    init {
        viewModelScope.launch(Dispatchers.IO) {
            nsdHelper.isServiceResolved.first { it }
            createClient()
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(Debugging.TAG, "ClientViewModel cleared")
    }

    fun discoverServiceWithGameCode(gameCode: String) {
        nsdHelper.discoverServiceWithGameCode(gameCode)
    }

    private suspend fun createClient() {
        val hostIpAddress: String = nsdHelper.hostIpAddress
        val hostPort: Int = nsdHelper.hostPort
        Log.d(Debugging.TAG, "Started client")
        Log.d(Debugging.TAG, "Address: $hostIpAddress Port: $hostPort")
        Client.run(
            hostIpAddress,
            hostPort,
            ::handleServerMessages
        )
    }

    // region Server messages handling
    private suspend fun handleServerMessages(
        connection: Connection
    ) {
        val gson = Gson()
        if (!Client.replay) {
            handleClientSideGameInitialization(connection, gson)
        } else {
            // TODO: This is to trigger hasFoundGame to true for now
            readLastPlayerFlagFromServer(connection, gson)
        }
        val categoryAndWordPair = readCategoryAndWordFromServer(connection, gson)
        val categoryOrdinal = categoryAndWordPair.first
        val wordResID = categoryAndWordPair.second
        val imposter = if (wordResID == -1) gameData.value.currentPlayer!! else null
        gameRepository.updateGameData(
            gameRepository.gameData.value.copy(
                categoryOrdinal = categoryOrdinal, wordResID = wordResID, imposter = imposter
            )
        )
        gameRepository.updateGameState(
            GameState.DisplayCategoryAndWord(
                gameRepository.gameData.value.categoryOrdinal,
                gameRepository.gameData.value.wordResID
            )
        )
        gameRepository.updateClientGameState(GameState.ClientGameStarted)
        gameState.first { it is GameState.ConfirmCurrentPlayerReadCategoryAndWord }
        sendCategoryAndWordConfirmationToServer(connection, gson)
        handleClientSideQuestionRoundsMessages(connection, gson)
        readStartVoteMessageFromServer(connection, gson)
        gameRepository.updateGameState(GameState.StartVote)
        sendVoteToServer(connection, gson)
        Log.d(Debugging.TAG, "Reading 'end vote' messages from server.")
        gameRepository.updateGameData(
            gameRepository.gameData.value.copy(
                imposter = readImposterPlayerFromServer(connection, gson),
                roundVotingCounts = readVotingResultsFromServer(connection),
                playerScores = readPlayerScoresFromServer(connection)
            )
        )
        val roundVotingCounts = gameRepository.gameData.value.roundVotingCounts
        gameRepository.updateGameState(GameState.EndVote(roundVotingCounts.maxBy { it.value }.key))
        Client.replay = readReplayFlagFromServer(connection, gson)
        gameRepository.updateGameState(GameState.Replay(Client.replay))
        // This is called regardless to end the game
        // TODO: Replace this delay when refactoring this
        delay(200L)
        playAnotherRound()
    }

    private suspend fun readReplayFlagFromServer(connection: Connection, gson: Gson): Boolean {
        Log.d(Debugging.TAG, "Reading 'replay' message from server...")
        val json = connection.input.readUTF8Line()
        val replay = gson.fromJson(json, Boolean::class.java)
        Log.d(Debugging.TAG, "Replay: $replay")
        return replay
    }

    private suspend fun readPlayerScoresFromServer(connection: Connection): MutableMap<Player, Int> {
        val votesType = object : TypeToken<Map<Player, Int>>() {}.type
        val json = connection.input.readUTF8Line()
        val complexGson = GsonBuilder().enableComplexMapKeySerialization().create()
        val playerScores =
            complexGson.fromJson<Map<Player, Int>>(json, votesType) as MutableMap<Player, Int>
        Log.d(Debugging.TAG, "Scores: $playerScores")
        return playerScores
    }

    private suspend fun readVotingResultsFromServer(connection: Connection): Map<Player, Int> {
        val votesType = object : TypeToken<Map<Player, Int>>() {}.type
        val json = connection.input.readUTF8Line()
        // This it to make key parsed as an object not string
        val complexGson = GsonBuilder().enableComplexMapKeySerialization().create()
        val votedPlayers = complexGson.fromJson<Map<Player, Int>>(json, votesType)
        Log.d(Debugging.TAG, "Voting results: $votedPlayers")
        return votedPlayers
    }

    private suspend fun readImposterPlayerFromServer(connection: Connection, gson: Gson): Player {
        val json = connection.input.readUTF8Line()
        val imposterPlayer = gson.fromJson(json, Player::class.java)
        Log.d(Debugging.TAG, "Imposter: $imposterPlayer")
        return imposterPlayer
    }

    private suspend fun sendVoteToServer(connection: Connection, gson: Gson) {
        gameState.first { it is GameState.GetCurrentPlayerVote }
        val votedPlayer = gameData.value.currentPlayerVotedPlayer!!
        Log.d(Debugging.TAG, "Voted Player: $votedPlayer")
        Log.d(Debugging.TAG, "Sending vote to server.")
        connection.output.writeLineUtf8(gson.toJson(votedPlayer))
    }

    private suspend fun readStartVoteMessageFromServer(connection: Connection, gson: Gson) {
        Log.d(Debugging.TAG, "Reading 'start vote' message from server...")
        val json: String? = connection.input.readUTF8Line()
        val startVoteFlag = gson.fromJson(json, Boolean::class.java)
        Log.d(Debugging.TAG, "Start vote flag: $startVoteFlag")
    }

    private suspend fun handleClientSideQuestionRoundsMessages(connection: Connection, gson: Gson) {
        do {
            handleClientSideQuestionsRoundMessages(connection, gson)
            gameRepository.updateGameState(GameState.ChooseExtraQuestions)
            val isAnotherQuestionsRound = readExtraQuestionsRoundFlagFromServer(connection, gson)
        } while (isAnotherQuestionsRound)
    }

    private suspend fun readExtraQuestionsRoundFlagFromServer(
        connection: Connection, gson: Gson
    ): Boolean {
        Log.d(Debugging.TAG, "Reading 'ask extra questions' message from server...")
        val isAnotherQuestionsRoundJson = connection.input.readUTF8Line()
        val isAnotherQuestionsRound =
            gson.fromJson(isAnotherQuestionsRoundJson, Boolean::class.java)
        Log.d(Debugging.TAG, "isAnotherQuestionsRound = $isAnotherQuestionsRoundJson")
        return isAnotherQuestionsRound
    }

    private suspend fun handleClientSideQuestionsRoundMessages(connection: Connection, gson: Gson) {
        var json: String?
        do {
            Log.d(Debugging.TAG, "Reading 'ask question' message from server...")
            json = connection.input.readUTF8Line()
            val askQuestionState = gson.fromJson(json, GameState.AskQuestion::class.java)
            gameRepository.updateGameState(askQuestionState)
            // TODO: This is to determine if this is the last question or not
            //      which is already sent by the server. This should be changed server side and
            //      then removed from here. It makes no sense since the Server sent the last question
            //      flag in the state it already sent, so why send it back to the server?
            // This is used to tell the server that the client has finished asking the question
            if (askQuestionState.isAsking) {
                gameState.first { it is GameState.ConfirmCurrentPlayerQuestion }
                Log.d(Debugging.TAG, "Sending 'isLast' message to server.")
                connection.output.writeLineUtf8(gson.toJson(askQuestionState.isLastQuestion))
            }
        } while (!(askQuestionState.isLastQuestion))
        Log.d(Debugging.TAG, "Reading 'end of questions round flag' message from server...")
        json = connection.input.readUTF8Line()
        val endOfQuestionsRoundFlag = gson.fromJson(json, Boolean::class.java)
        Log.d(Debugging.TAG, "endOfQuestionsRoundFlag = $endOfQuestionsRoundFlag")
    }

    private suspend fun sendCategoryAndWordConfirmationToServer(
        connection: Connection, gson: Gson
    ) {
        Log.d(Debugging.TAG, "Sending confirm reading category and word message to server")
        connection.output.writeLineUtf8(gson.toJson(true))
    }

    private suspend fun readCategoryAndWordFromServer(
        connection: Connection, gson: Gson
    ): Pair<Int, Int> {
        Log.d(Debugging.TAG, "Reading 'category and word' message from server...")
        val type = object : TypeToken<Map<String, String>>() {}.type
        val json = connection.input.readUTF8Line()
        if (json != null) {
            Log.d(Debugging.TAG, json.ifEmpty { "Empty" })
        }
        val categoryAndWord = gson.fromJson<Map<String, String>>(json, type)
        Log.d(
            Debugging.TAG,
            "Category: ${Categories.values()[categoryAndWord["category"]!!.toInt()]}, Word: ${categoryAndWord["word"] ?: "IMPOSTER"}"
        )
        val categoryOrdinal = categoryAndWord["category"]!!.toInt()
        val wordResID = categoryAndWord["word"]?.toInt() ?: -1
        return Pair(categoryOrdinal, wordResID)
    }

    private suspend fun handleClientSideGameInitialization(
        connection: Connection, gson: Gson
    ) {
        sendPlayerNameToServer(connection, gson)
        val playerColor: String = readPlayerColorFromServer(connection, gson)
        gameRepository.updateGameData(
            gameData.value.copy(
                currentPlayer = gameData.value.currentPlayer!!.copy(color = playerColor)
            )
        )
        readPlayersListUpdatesFromServer(connection, gson)
    }

    private suspend fun readPlayersListUpdatesFromServer(
        connection: Connection, gson: Gson
    ) {
        // This will always be false the first time it is sent to any client.
        // As it will be sent right after the player joins; after sending the player color.
        // Read players list once and go to lobby (Game Found!)
        // TODO: The problem is here probably check when last player flag is sent and other booleans
        //  and count the number of reads and sends and check if they are correct
        @Suppress("VARIABLE_WITH_REDUNDANT_INITIALIZER") var isLastPlayer =
            readLastPlayerFlagFromServer(connection, gson)
        readPlayersListUpdateFromServer(connection, gson)
        gameRepository.updateClientGameState(GameState.ClientFoundGame)
        isLastPlayer = readLastPlayerFlagFromServer(connection, gson)
        while (!isLastPlayer) {
            readPlayersListUpdateFromServer(connection, gson)
            isLastPlayer = readLastPlayerFlagFromServer(connection, gson)
        }
    }

    private suspend fun readLastPlayerFlagFromServer(connection: Connection, gson: Gson): Boolean {
        Log.d(Debugging.TAG, "Reading 'isLastPlayer' message from server...")
        val isLastPlayerJson = connection.input.readUTF8Line()
        val isLastPlayer = gson.fromJson(isLastPlayerJson, Boolean::class.java)
        Log.d(Debugging.TAG, "isLastPlayer = $isLastPlayer")
        return isLastPlayer
    }

    private suspend fun readPlayersListUpdateFromServer(
        connection: Connection, gson: Gson
    ) {
        Log.d(Debugging.TAG, "Reading 'players' message from server...")
        val collectionType = object : TypeToken<Collection<Player>>() {}.type
        val jsonPlayers = connection.input.readUTF8Line()
        val newPlayers: List<Player> = gson.fromJson(jsonPlayers, collectionType)
        gameRepository.updateGameData(gameRepository.gameData.value.copy(players = newPlayers))
        Log.d(Debugging.TAG, "Players: ${gameRepository.gameData.value.players}")
    }

    private suspend fun readPlayerColorFromServer(connection: Connection, gson: Gson): String {
        Log.d(Debugging.TAG, "Reading 'player color' message from server...")
        val json = connection.input.readUTF8Line()
        return gson.fromJson(json, String::class.java)
    }

    private suspend fun sendPlayerNameToServer(connection: Connection, gson: Gson) {
        Log.d(Debugging.TAG, "Sending player name to server.")
        connection.output.writeLineUtf8(gson.toJson(gameData.value.currentPlayer!!.name))
    }

    // endregion
    private fun playAnotherRound() {
        resetRoundParameters()
        resetClientGameState()
    }

    private fun resetRoundParameters() {
        gameRepository.updateGameState(GameState.StartGame)
        val oldGameData = gameData.value
        gameRepository.updateGameData(
            GameData(
                gameCode = oldGameData.gameCode,
                isHost = oldGameData.isHost,
                currentPlayer = oldGameData.currentPlayer,
                isFirstRound = false,
                players = oldGameData.players,
                playerScores = oldGameData.playerScores,
            )
        )
    }

    private fun resetClientGameState() {
        gameRepository.updateClientGameState(null)
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        class ClientViewModelFactory(
            private val gameRepository: GameRepository,
            private val nsdHelper: NSDHelper,
            private val clientNetworkRepository: ClientNetworkRepository,
        ) : ViewModelProvider.NewInstanceFactory() {
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ClientViewModel(gameRepository, nsdHelper, clientNetworkRepository) as T
        }
    }
}
