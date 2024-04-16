package com.example.nsddemo.ui

import android.content.SharedPreferences
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.text.format.Formatter
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nsddemo.Categories
import com.example.nsddemo.Debugging.TAG
import com.example.nsddemo.GameConstants
import com.example.nsddemo.GameRepository
import com.example.nsddemo.GameState
import com.example.nsddemo.NSDConstants.BASE_SERVICE_NAME
import com.example.nsddemo.NSDConstants.SERVICE_TYPE
import com.example.nsddemo.Player
import com.example.nsddemo.StateManager
import com.example.nsddemo.network.Client
import com.example.nsddemo.network.Server
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import io.ktor.network.sockets.Connection
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.readUTF8Line
import io.ktor.utils.io.writeStringUtf8
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class GameViewModel(
    val nsdManager: NsdManager,
    val wifiManager: WifiManager,
    val sharedPreferences: SharedPreferences,
    val stateManager: StateManager,
    val gameRepository: GameRepository,
) : ViewModel() {
    // region Properties
    private var mServiceName: String = BASE_SERVICE_NAME

    // TODO: Move to CreateGameViewModel
    private val _isGameCreated = MutableStateFlow(false)
    val isGameCreated = _isGameCreated.asStateFlow()

    val gameState = gameRepository.gameState
    val gameData = gameRepository.gameData

    private lateinit var clientJob: Job
    private lateinit var serverJob: Job
    private lateinit var gameStateCollectorJob: Job
    // endregion

    // region Service registration
    val onRegisterServiceClick = {
        Log.d(TAG, "Registering service...")
        gameStateCollectorJob = stateManager.reactToGameStateChanges(viewModelScope)
        val isHost = true
        val gameCode = generateGameCode()
        gameRepository.updateGameData(
            gameRepository.gameData.value.copy(
                isHost = isHost, gameCode = gameCode
            )
        )
        Log.d(TAG, "gameCode: ${gameRepository.gameData.value.gameCode}")
        var serverIP: String? = null
        try {
            serverIP = Formatter.formatIpAddress(wifiManager.connectionInfo.ipAddress)
            Log.d(TAG, "Device IP: $serverIP")
        } catch (e: Exception) {
            Log.d(TAG, e.message.toString())
        }
        val serverPort: Int = Server.initServerSocket(serverIP!!)
        serverJob = viewModelScope.launch(Dispatchers.IO) {
            Log.d(TAG, "Started server")
            Server.run(::handleClientMessages)
        }
        Log.d(TAG, serverJob.children.toString())
        registerService(serverPort, gameRepository.gameData.value.gameCode!!)
    }
    private val registrationListener = object : NsdManager.RegistrationListener {
        override fun onServiceRegistered(NsdServiceInfo: NsdServiceInfo) {
            // Save the service name. Android may have changed it in order to
            // resolve a conflict, so update the name you initially requested
            // with the name Android actually used.
            mServiceName = NsdServiceInfo.serviceName
            Log.d(TAG, "Service address: ${NsdServiceInfo.host} ${NsdServiceInfo.port}")
            Log.d(TAG, "onServiceRegistered: serviceName = $mServiceName")
            _isGameCreated.value = true
        }

        override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            // Registration failed! Put debugging code here to determine why.
            Log.e(TAG, "onRegistrationFailed, Reason: $errorCode")
        }

        override fun onServiceUnregistered(arg0: NsdServiceInfo) {
            // Service has been unregistered. This only happens when you call
            // NsdManager.unregisterService() and pass in this listener.
            Log.d(TAG, "onServiceUnregistered")
        }

        override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            // Unregistration failed. Put debugging code here to determine why.
            Log.e(TAG, "onUnregistrationFailed, Reason: $errorCode")
        }
    }

    private fun registerService(port: Int, gameCode: String) {
        // Create the NsdServiceInfo object, and populate it.
        val serviceInfo = NsdServiceInfo().apply {
            // The name is subject to change based on conflicts
            // with other services advertised on the same network.
            serviceName =
                if (gameRepository.gameData.value.isHost!!) mServiceName + "_$gameCode" else mServiceName
            serviceType = SERVICE_TYPE
            setPort(port)
        }
        Log.d(TAG, "Created serviceInfo")
        try {
            nsdManager.apply {
                this.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
            }
        } catch (E: Exception) {
            Log.e(TAG, E.message.toString())
        } finally {

        }
        Log.d(TAG, "Created nsdManager")
    }
    // endregion

    // region UI-related functions and properties

    // TODO: Move to DisplayCategoryAndWordViewModel
    private val _isConfirmButtonPressed = MutableStateFlow(false)
    val isConfirmButtonPressed = _isConfirmButtonPressed.asStateFlow()

    val onConfirmClick = fun() {
        _isConfirmButtonPressed.value = true
        when (val currentGameState = gameRepository.gameState.value) {
            is GameState.GetPlayerReadCategoryAndWordConfirmation -> {
                gameRepository.updateGameState(
                    GameState.ConfirmCurrentPlayerReadCategoryAndWord(currentGameState.numberOfConfirmations + 1)
                )
            }

            else -> {
                gameRepository.updateGameState(GameState.ConfirmCurrentPlayerReadCategoryAndWord(1))
            }
        }
    }

    private val _wordDialogVisibilityState = mutableStateOf(false)
    val wordDialogVisibilityState: State<Boolean> = _wordDialogVisibilityState

    val onShowWordClick = {
        _wordDialogVisibilityState.value = true
    }

    val onWordDialogOkClicked = {
        _wordDialogVisibilityState.value = false
    }

    // TODO: Move to QuestionsViewModel
    private val _isDoneClicked = MutableStateFlow(false)
    val isDoneClicked = _isDoneClicked.asStateFlow()

    val onDoneClick = fun() {
        _isDoneClicked.value = true
        val currentGameState = gameRepository.gameState.value as GameState.AskQuestion
        gameRepository.updateGameState(GameState.ConfirmCurrentPlayerQuestion(currentGameState))
    }

    val onRestartQuestionsClick = fun() {
        stateManager.startNewQuestionsRound()
    }

    val onStartVoteClick = {
        gameRepository.updateGameState(GameState.StartVote)
    }

    private val _votedPlayer = mutableStateOf<Player?>(null)
    val votedPlayer: State<Player?> = _votedPlayer

    val onVoteForPlayer = { player: Player ->
        _votedPlayer.value = player
    }
    val _isVotedClicked = MutableStateFlow(false)
    val isVotedClicked = _isVotedClicked.asStateFlow()

    val onConfirmVoteClick = {
        _isVotedClicked.value = true
        gameRepository.updateGameState(GameState.GetCurrentPlayerVote(votedPlayer.value!!))
    }

    val onShowScoreClick = {
        gameRepository.updateGameState(GameState.ShowScoreboard)
    }

    val onEndGameClick = {
        gameRepository.updateGameState(GameState.Replay(false))
    }

    val onReplayClick = {
        gameRepository.updateGameState(GameState.Replay(true))
    }

    fun onJoinGamePressed(gameCode: String) {
        gameRepository.updateGameData(
            gameRepository.gameData.value.copy(
                gameCode = gameCode.uppercase()
            )
        )
    }

    // endregion
    init {
        Log.d(TAG, "GameViewModel created.")
        val defaultPlayerName = ""
        val savedPlayerName =
            sharedPreferences.getString(PLAYER_NAME_SHARED_PREF_KEY, defaultPlayerName)!!
        if (savedPlayerName != defaultPlayerName) {
            gameRepository.updateGameData(
                gameRepository.gameData.value.copy(
                    currentPlayer = Player(savedPlayerName, GameConstants.DEFAULT_PLAYER_COLOR)
                )
            )
        }
        Log.d(TAG, "Current player: ${gameData.value.currentPlayer}")
    }

    // region Server messages handling

    suspend fun handleServerMessages(
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
        Log.d(TAG, "Reading 'end vote' messages from server.")
        gameRepository.updateGameData(
            gameRepository.gameData.value.copy(
                imposter = readImposterPlayerFromServer(connection, gson)
            )
        )
        gameRepository.updateGameData(
            gameRepository.gameData.value.copy(
                roundVotingCounts = readVotingResultsFromServer(connection),
                playerScores = readPlayerScoresFromServer(connection)
            )
        )
        val roundVotingCounts = gameRepository.gameData.value.roundVotingCounts
        gameRepository.updateGameState(GameState.EndVote(roundVotingCounts.maxBy { it.value }.key))
        Client.replay = readReplayFlagFromServer(connection, gson)
        // This is called regardless to end the game
        replayGame()
        gameRepository.updateGameState(GameState.Replay(Client.replay))
    }

    private suspend fun readReplayFlagFromServer(connection: Connection, gson: Gson): Boolean {
        Log.d(TAG, "Reading 'replay' message from server...")
        val json = connection.input.readUTF8Line()
        val replay = gson.fromJson(json, Boolean::class.java)
        Log.d(TAG, "Replay: $replay")
        return replay
    }

    private suspend fun readPlayerScoresFromServer(connection: Connection): MutableMap<Player, Int> {
        val votesType = object : TypeToken<Map<Player, Int>>() {}.type
        val json = connection.input.readUTF8Line()
        val complexGson = GsonBuilder().enableComplexMapKeySerialization().create()
        val playerScores =
            complexGson.fromJson<Map<Player, Int>>(json, votesType) as MutableMap<Player, Int>
        Log.d(TAG, "Scores: $playerScores")
        return playerScores
    }

    private suspend fun readVotingResultsFromServer(connection: Connection): Map<Player, Int> {
        val votesType = object : TypeToken<Map<Player, Int>>() {}.type
        val json = connection.input.readUTF8Line()
        // This it to make key parsed as an object not string
        val complexGson = GsonBuilder().enableComplexMapKeySerialization().create()
        val votedPlayers = complexGson.fromJson<Map<Player, Int>>(json, votesType)
        Log.d(TAG, "Voting results: $votedPlayers")
        return votedPlayers
    }

    private suspend fun readImposterPlayerFromServer(connection: Connection, gson: Gson): Player {
        val json = connection.input.readUTF8Line()
        val imposterPlayer = gson.fromJson(json, Player::class.java)
        Log.d(TAG, "Imposter: $imposterPlayer")
        return imposterPlayer
    }

    private suspend fun sendVoteToServer(connection: Connection, gson: Gson) {
        gameState.first { it is GameState.GetCurrentPlayerVote }
        Log.d(TAG, "Voted Player: ${votedPlayer.value}")
        Log.d(TAG, "Sending vote to server.")
        connection.output.writeLineUtf8(gson.toJson(votedPlayer.value))
    }

    private suspend fun readStartVoteMessageFromServer(connection: Connection, gson: Gson) {
        Log.d(TAG, "Reading 'start vote' message from server...")
        val json: String? = connection.input.readUTF8Line()
        val startVoteFlag = gson.fromJson(json, Boolean::class.java)
        Log.d(TAG, "Start vote flag: $startVoteFlag")
    }

    private suspend fun handleClientSideQuestionRoundsMessages(connection: Connection, gson: Gson) {
        do {
            handleClientSideQuestionsRoundMessages(connection, gson)
            // TODO: Replace this with ChooseExtraQuestions state
            gameRepository.updateGameState(GameState.ChooseExtraQuestions)
            val isAnotherQuestionsRound = readExtraQuestionsRoundFlagFromServer(connection, gson)
        } while (isAnotherQuestionsRound)
    }

    private suspend fun readExtraQuestionsRoundFlagFromServer(
        connection: Connection, gson: Gson
    ): Boolean {
        Log.d(TAG, "Reading 'ask extra questions' message from server...")
        val isAnotherQuestionsRoundJson = connection.input.readUTF8Line()
        val isAnotherQuestionsRound =
            gson.fromJson(isAnotherQuestionsRoundJson, Boolean::class.java)
        Log.d(TAG, "isAnotherQuestionsRound = $isAnotherQuestionsRoundJson")
        return isAnotherQuestionsRound
    }

    private suspend fun handleClientSideQuestionsRoundMessages(connection: Connection, gson: Gson) {
        var json: String?
        do {
            Log.d(TAG, "Reading 'ask question' message from server...")
            json = connection.input.readUTF8Line()
            Log.d(TAG, "$json")
            val askQuestionState = gson.fromJson(json, GameState.AskQuestion::class.java)
            gameRepository.updateGameState(askQuestionState)
            // TODO: This is to determine if this is the last question or not
            //      which is already sent by the server. This should be changed server side and
            //      then removed from here. It makes no sense since the Server sent the last question
            //      flag in the state it already sent, so why send it back to the server?
            // This is used to tell the server that the client has finished asking the question
            if (askQuestionState.isAsking) {
                gameState.first { it is GameState.ConfirmCurrentPlayerQuestion }
                Log.d(TAG, "Sending 'isLast' message to server.")
                connection.output.writeStringUtf8(gson.toJson(askQuestionState.isLastQuestion) + '\n')
            }
        } while (!(askQuestionState.isLastQuestion))
        Log.d(TAG, "Reading 'end of questions round flag' message from server...")
        json = connection.input.readUTF8Line()
        val endOfQuestionsRoundFlag = gson.fromJson(json, Boolean::class.java)
        Log.d(TAG, "endOfQuestionsRoundFlag = $endOfQuestionsRoundFlag")
    }

    private suspend fun sendCategoryAndWordConfirmationToServer(
        connection: Connection, gson: Gson
    ) {
        Log.d(TAG, "Sending confirm reading category and word message to server")
        connection.output.writeStringUtf8(gson.toJson(true) + '\n')
    }

    private suspend fun readCategoryAndWordFromServer(
        connection: Connection, gson: Gson
    ): Pair<Int, Int> {
        Log.d(TAG, "Reading 'category and word' message from server...")
        val type = object : TypeToken<Map<String, String>>() {}.type
        val json = connection.input.readUTF8Line()
        val categoryAndWord = gson.fromJson<Map<String, String>>(json, type)
        Log.d(
            TAG,
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
        Log.d(TAG, "Reading 'isLastPlayer' message from server...")
        val isLastPlayerJson = connection.input.readUTF8Line()
        val isLastPlayer = gson.fromJson(isLastPlayerJson, Boolean::class.java)
        Log.d(TAG, "isLastPlayer = $isLastPlayer")
        return isLastPlayer
    }

    private suspend fun readPlayersListUpdateFromServer(
        connection: Connection, gson: Gson
    ) {
        Log.d(TAG, "Reading 'players' message from server...")
        val collectionType = object : TypeToken<Collection<Player>>() {}.type
        val jsonPlayers = connection.input.readUTF8Line()
        val newPlayers: List<Player> = gson.fromJson(jsonPlayers, collectionType)
        gameRepository.updateGameData(gameRepository.gameData.value.copy(players = newPlayers))
        Log.d(TAG, "Players: ${gameRepository.gameData.value.players}")
    }

    private suspend fun readPlayerColorFromServer(connection: Connection, gson: Gson): String {
        Log.d(TAG, "Reading 'player color' message from server...")
        val json = connection.input.readUTF8Line()
        return gson.fromJson(json, String::class.java)
    }

    private suspend fun sendPlayerNameToServer(connection: Connection, gson: Gson) {
        Log.d(TAG, "Sending player name to server.")
        connection.output.writeLineUtf8(gson.toJson(gameData.value.currentPlayer!!.name))
    }
    // endregion

    // region Client messages handling
    private suspend fun handleClientMessages(connection: Connection) {
        Log.d(TAG, "Waiting for client message...")
        val json = connection.input.readUTF8Line()
        Log.d(TAG, "==========Message received on state: ${gameState.value}==========")
        Log.d(TAG, "Received from client: $json")
        if (json == null) {
            throw IllegalArgumentException("Received null string from client.")
        }
        val gson = Gson()
        updateStateOnMessageReceivedServerSide(json, connection, gson)
    }

    private fun updateStateOnMessageReceivedServerSide(
        json: String,
        connection: Connection,
        gson: Gson
    ) {
        when (val currentGameState = gameState.value) {
            GameState.StartGame, is GameState.GetPlayerInfo -> {
                val playerName = gson.fromJson(json, String::class.java)
                gameRepository.updateGameState(GameState.GetPlayerInfo(playerName, connection))
            }

            GameState.ClientFoundGame -> {
                Log.wtf(
                    TAG,
                    "Received a message while in GameState.ClientFoundGame. Server can't be in this state!"
                )
            }

            GameState.ClientGameStarted -> {
                Log.wtf(
                    TAG,
                    "Received a message while in GameState.ClientGameStarted. Server can't be in this state!"
                )
            }

            is GameState.DisplayCategoryAndWord -> {
                val isConfirmed = gson.fromJson(json, Boolean::class.java)
                Log.d(TAG, "isConfirmed: $isConfirmed")
                gameRepository.updateGameState(
                    GameState.GetPlayerReadCategoryAndWordConfirmation(1)
                )
            }

            // is GameState.GetPlayerReadCategoryAndWordConfirmation, is GameState.ConfirmCurrentPlayerReadCategoryAndWord
            is GameState.ConfirmReadCategoryAndWord -> {
                val isConfirmed = gson.fromJson(json, Boolean::class.java)
                Log.d(TAG, "isConfirmed: $isConfirmed")
                gameRepository.updateGameState(
                    GameState.GetPlayerReadCategoryAndWordConfirmation(currentGameState.numberOfConfirmations + 1)
                )
            }

            is GameState.AskQuestion -> {
                // TODO: this and askNextQuestionOrGoToExtraQuestionsChoice in StateManager should be merged
                // This is sent from the player asking, confirming to end question
                val isLastQuestionConfirmation = gson.fromJson(json, Boolean::class.java)
                Log.d(TAG, "isLastQuestion = $isLastQuestionConfirmation received.")
                if (isLastQuestionConfirmation) {
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

            is GameState.ConfirmCurrentPlayerQuestion -> {
                Log.wtf(TAG, "Received a message while in GameState.ConfirmCurrentPlayerQuestion.")
            }

            GameState.ChooseExtraQuestions -> {
                Log.wtf(TAG, "Received a message while in GameState.ChooseExtraQuestions.")
            }

            GameState.AskExtraQuestions -> {
                Log.wtf(TAG, "Received a message while in GameState.AskExtraQuestions.")
            }

            GameState.StartVote, is GameState.GetPlayerVote, is GameState.GetCurrentPlayerVote -> {
                val votedPlayer = gson.fromJson(json, Player::class.java)
                gameRepository.updateGameState(
                    GameState.GetPlayerVote(
                        Server.clients[connection]!!, votedPlayer
                    )
                )
            }

            is GameState.EndVote -> {
                Log.wtf(TAG, "Received a message while in GameState.EndVote.")
            }

            GameState.ShowScoreboard -> {
                Log.wtf(TAG, "Received a message while in GameState.ShowScoreboard.")
            }

            is GameState.Replay -> {
                Log.wtf(TAG, "Received a message while in GameState.Replay.")
            }
        }
    }
    // endregion

    /**
     * Generates a [List] of [Player] pairs such that each [Player] asks and is asked exactly once.
     */

    // region General utility functions
    private fun generateGameCode(): String {
        val allowedChars = ('A'..'Z') + ('0'..'9')
        return (1..GameConstants.CODE_LENGTH).map { allowedChars.random() }.joinToString("")
    }

    fun replayGame() {
        resetRoundParameters()
        resetUIStates()
    }

    private fun resetRoundParameters() {
        gameRepository.updateGameState(GameState.StartGame)
        gameRepository.updateGameData(
            gameRepository.gameData.value.copy(
                currentPlayerPairIndex = 0,
                roundPlayerPairs = emptyList(),
                roundPlayerVotes = emptyMap(),
                roundVotingCounts = emptyMap(),
                imposter = null,
                categoryOrdinal = -1,
                wordResID = -1,
            )
        )
    }

    private fun resetGameParameters() {
        if (gameData.value.isHost!!) {
            gameRepository.updateGameData(
                gameRepository.gameData.value.copy(
                    isFirstRound = true
                )
            )
            Server.clients.clear()
        }
        gameRepository.updateGameData(
            gameRepository.gameData.value.copy(
                isHost = false,
                playerScores = emptyMap(),
                currentPlayer = gameData.value.currentPlayer!!.copy(color = GameConstants.DEFAULT_PLAYER_COLOR),
                players = emptyList(),
            )
        )
        mServiceName = BASE_SERVICE_NAME
    }

    fun resetUIStates() {
        // ClientGameState
        gameRepository.updateClientGameState(null)
        // CreateGame Screen
        //TODO: Maybe this shouldn't be reset
        _isGameCreated.value = false
        //DisplayCategoryAndWord Screen
        _isConfirmButtonPressed.value = false
        // Questions Screen
        _isDoneClicked.value = false
        // Voting Screen
        _votedPlayer.value = null
        _isVotedClicked.value = false
    }

    fun updateCurrentPlayer(player: Player) {
        gameRepository.updateGameData(
            gameRepository.gameData.value.copy(
                currentPlayer = player
            )
        )
        with(sharedPreferences.edit()) {
            putString(PLAYER_NAME_SHARED_PREF_KEY, player.name)
            apply()
        }
    }

    fun actAsClient() {
        gameRepository.updateGameData(
            gameRepository.gameData.value.copy(
                isHost = false
            )
        )
    }

    fun setClientJob(job: Job) {
        clientJob = job
    }

    fun endGame() {
        if (gameData.value.isHost!!) {
            serverJob.cancel()
            gameStateCollectorJob.cancel()
            nsdManager.unregisterService(registrationListener)
        } else {
            clientJob.cancel()
        }
        replayGame()
        resetGameParameters()
    }

    fun chooseRandomWord(chosenCategory: Categories) {
//        chooseCurrentPlayerColor()
        val categoryOrdinal = chosenCategory.ordinal
        val wordResID = chosenCategory.wordResourceIds.random()
        gameRepository.updateGameData(
            gameRepository.gameData.value.copy(
                categoryOrdinal = categoryOrdinal, wordResID = wordResID
            )
        )
        gameRepository.updateGameState(
            GameState.DisplayCategoryAndWord(
                categoryOrdinal, wordResID
            )
        )
    }

    private fun chooseCurrentPlayerColor() {
        // TODO: Remove this it's useless
        if (!gameData.value.isFirstRound) return
        val selectedPlayerColors = gameRepository.gameData.value.selectedPlayerColors
        val currentPlayerColor =
            PlayerColors.values().filter { it !in selectedPlayerColors }.random()
        Log.d(TAG, "Current player color: $currentPlayerColor: ${currentPlayerColor.argb}")
        gameRepository.updateGameData(
            gameRepository.gameData.value.copy(
                currentPlayer = Player(
                    gameData.value.currentPlayer!!.name, currentPlayerColor.argb.toString()
                )
            )
        )
    }
    // endregion

    // region Message utility functions
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
        private const val PLAYER_SCORE_INCREMENT = 100
        private const val PLAYER_NAME_SHARED_PREF_KEY = "impostle_player_name"
    }
}