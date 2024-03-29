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
import com.example.nsddemo.GameData
import com.example.nsddemo.GameState
import com.example.nsddemo.NSDConstants.BASE_SERVICE_NAME
import com.example.nsddemo.NSDConstants.SERVICE_TYPE
import com.example.nsddemo.Player
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
    val gameData: GameData
) : ViewModel() {
    // region Properties
    private var mServiceName: String = BASE_SERVICE_NAME
    private val _isGameCreated = MutableStateFlow(false)
    val isGameCreated = _isGameCreated.asStateFlow()

    private val _hasJoinedGame = MutableStateFlow(false)
    val hasJoinedGame = _hasJoinedGame.asStateFlow()

    private val _gameState: MutableStateFlow<GameState> = MutableStateFlow(GameState.StartGame)
    val gameState = _gameState.asStateFlow()

    private var numberOfReadCategoryAndWordConfirmations = 0
    private lateinit var askingPairs: List<Pair<Player, Player>>
    private var currentAskingPairIndex = 0
    private var numberOfVoters = 0
    private var _votedPlayers: MutableMap<Player, Int> = mutableMapOf()
    val votedPlayers: Map<Player, Int>
        get() {
            return _votedPlayers.toMap()
        }

    private val playerVotes: MutableMap<Player, Player> = mutableMapOf()

    private var _playerScores: MutableMap<Player, Int> = mutableMapOf()
    val playerScores: Map<Player, Int>
        get() {
            return _playerScores
        }

    //TODO: It should be sent to the clients as response to their player name message
    private val _currentPlayer = MutableStateFlow(gameData.currentPlayer)
    val currentPlayer = _currentPlayer.asStateFlow()
    lateinit var imposterPlayer: Player
        private set
    var isImposter: Boolean? = null
        private set
    private val _players = MutableStateFlow(emptyList<Player>())
    val players = _players.asStateFlow()
    private val selectedPlayerColors = mutableListOf<PlayerColors>()
    private lateinit var clientJob: Job
    private lateinit var serverJob: Job
    private lateinit var gameStateCollectorJob: Job
    private var isFirstRound = true
    var categoryOrdinal: Int = -1
        private set
    var wordResId: Int = -1
        private set
    // endregion

    // region Service registration
    val onRegisterServiceClick = {
        gameStateCollectorJob = viewModelScope.launch(Dispatchers.IO) {
            val gson = Gson()
            gameState.collect {
                when (val currentGameState = gameState.value) {
                    GameState.StartGame -> handleStartGameState()

                    is GameState.GetPlayerInfo -> handleGetPlayerInfoState(currentGameState, gson)

                    // This game state will happen when the user which acts as the server
                    // presses a button like "start game"
                    is GameState.DisplayCategoryAndWord -> handleDisplayCategoryAndWordState(
                        currentGameState, gson
                    )

                    is GameState.GetPlayerReadCategoryAndWordConfirmation -> handleGetPlayerReadCategoryAndWordConfirmationState()

                    is GameState.AskQuestion -> handleAskQuestionState(currentGameState, gson)

                    GameState.AskExtraQuestions -> handleAskExtraQuestionsState(gson)

                    // This game state will happen when the user which acts as the server
                    // presses a button like "start vote"
                    // (The screen will be like any additional questions?)
                    GameState.StartVote -> handleStartVoteState(gson)

                    is GameState.GetPlayerVote -> handleGetPlayerVoteState(currentGameState)

                    //This event is triggered when all players have voted
                    // Should send the votes list to all players
                    is GameState.EndVote -> handleEndVoteState(gson)

                    // Triggered by user pressing a button (Should probably be sent with the votes list to
                    // be ready to be shown on screen right after to all players
                    GameState.ShowScoreboard -> handleShowScoreboardState()

                    // - For now this should send a message to all clients informing them
                    // that the game will be continued.
                    // - For now also this will be determined by the server, no voting.
                    is GameState.Replay -> handleReplayState(currentGameState, gson)
                }
            }
        }
        gameData.isHost = true
        gameData.gameCode = generateGameCode()
        Log.d(TAG, "gameCode: ${gameData.gameCode}")
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
        registerService(serverPort, gameData.gameCode!!)
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
            serviceName = if (gameData.isHost!!) mServiceName + "_$gameCode" else mServiceName
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

    // region GameState handling
    private suspend fun handleReplayState(currentGameState: GameState.Replay, gson: Gson) {
        Log.d(TAG, "Sending replay status to all players.")
        sendUtf8LineToAllPlayers {
            gson.toJson(currentGameState.replay)
        }
        isFirstRound = false
    }

    private fun handleShowScoreboardState() {
        TODO("Not yet implemented")
    }

    private suspend fun handleEndVoteState(gson: Gson) {
        val complexGson = GsonBuilder().enableComplexMapKeySerialization().create()
        for ((voter, voted) in playerVotes) {
            _playerScores[voter] = (_playerScores[voter]
                ?: 0) + if (voted == imposterPlayer) PLAYER_SCORE_INCREMENT else 0
        }
        sendUtf8LineToAllPlayers {
            gson.toJson(imposterPlayer)
        }
        sendUtf8LineToAllPlayers {
            complexGson.toJson(_votedPlayers)
        }
        sendUtf8LineToAllPlayers {
            complexGson.toJson(_playerScores)
        }
    }

    private fun handleGetPlayerVoteState(currentGameState: GameState.GetPlayerVote) {
        Log.d(
            TAG, "${currentGameState.voter} voted for ${currentGameState.voted}"
        )
        val votedPlayer = currentGameState.voted
        _votedPlayers[votedPlayer] = (_votedPlayers[votedPlayer] ?: 0) + 1
        playerVotes[currentGameState.voter] = currentGameState.voted
        numberOfVoters++
        Log.d(
            TAG, "numberOfVoters=$numberOfVoters, numOfPlayers=${Server.players.size + 1}"
        )
        if (numberOfVoters == Server.players.size + 1) {
            Log.d(TAG, "All players voted. Ending vote...")
            _gameState.value = GameState.EndVote(_votedPlayers.maxBy { it.value }.key)
        }
    }

    private suspend fun handleStartVoteState(gson: Gson) {
        // This is sent to exit "additional questions" loop on client side
        Log.d(
            TAG, "Start Vote: Sending 'additional questions' FALSE message to all players..."
        )
        sendUtf8LineToAllPlayers {
            gson.toJson(false)
        }
        Log.d(
            TAG, "Start Vote: Sending list of players to each player..."
        )
        val allPlayers = Server.players.values.toMutableList()
        allPlayers.add(_currentPlayer.value!!)
        Log.d(TAG, allPlayers.toString())
        sendUtf8LineToAllPlayers { player ->
            gson.toJson(allPlayers.filter { it != player })
        }
    }

    private suspend fun handleAskExtraQuestionsState(gson: Gson) {
        Log.d(TAG, "Ask Extra Questions: Sending TRUE message to clients")
        for ((clientConnection, _) in Server.players) {
            clientConnection.output.writeStringUtf8(gson.toJson(true) + '\n')
        }
    }

    private suspend fun handleAskQuestionState(
        currentGameState: GameState.AskQuestion, gson: Gson
    ) {
        if (currentGameState.isFirstQuestionInNewRound) {
            Log.d(
                TAG, "Sending 'additional questions' TRUE message to all players..."
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
        currentAskingPairIndex++
    }

    private fun handleGetPlayerReadCategoryAndWordConfirmationState() {
        numberOfReadCategoryAndWordConfirmations++
        Log.d(TAG, "# of Confirmations= $numberOfReadCategoryAndWordConfirmations")
        Log.d(TAG, "Server.players.size + 1= ${Server.players.size + 1}")
        if (numberOfReadCategoryAndWordConfirmations == Server.players.size + 1) {
            Log.d(
                TAG, "# of confirmations = $numberOfReadCategoryAndWordConfirmations"
            )
            // Ask first question
            _gameState.value = GameState.AskQuestion(
                askingPairs[currentAskingPairIndex].first,
                askingPairs[currentAskingPairIndex].second,
                (askingPairs[currentAskingPairIndex].first == _currentPlayer.value),
                currentAskingPairIndex == askingPairs.lastIndex
            )
        }
    }

    private suspend fun handleDisplayCategoryAndWordState(
        currentGameState: GameState.DisplayCategoryAndWord, gson: Gson
    ) {
        // Send a message to players indicating that the lastPlayer has joined
        // and the game is ready to start
        // TODO: This should only be sent when the game isn't being replayed.
        //  it should be in the game state itself
        //  IDEA: Maybe pass gamelobby object to each state and modify it properly.
        //  This way it's easier to handle the game state and the game lobby
        sendUtf8LineToAllPlayers { gson.toJson(true) }
        val playersIncludingServer = Server.players.values.toMutableList()
        playersIncludingServer.add(_currentPlayer.value!!)
        Log.d(TAG, playersIncludingServer.toString())
        askingPairs = generateAllAskingCombinations(playersIncludingServer)
        imposterPlayer = playersIncludingServer.random()
        Log.d(TAG, "Imposter: $imposterPlayer")
        isImposter = (imposterPlayer == _currentPlayer.value)
        categoryOrdinal = currentGameState.categoryOrdinal
        wordResId = currentGameState.wordResourceId
        sendUtf8LineToAllPlayers { player ->
            gson.toJson(
                if (player == imposterPlayer) mapOf("category" to categoryOrdinal)
                else mapOf("category" to categoryOrdinal, "word" to wordResId)
            )
        }
    }

    private suspend fun handleGetPlayerInfoState(
        currentGameState: GameState.GetPlayerInfo, gson: Gson
    ) {
        val playerColor = PlayerColors.values().filter { it !in selectedPlayerColors }.random()
        selectedPlayerColors.add(playerColor)
        val playerConnection = currentGameState.connection
        val currentPlayer = Player(currentGameState.name, playerColor.argb.toString())
        Server.players[playerConnection] = currentPlayer
        _players.value = Server.players.values.toList()
        playerConnection.output.writeLineUtf8(
            gson.toJson(playerColor.argb.toString())
        )
        // Send players list to all player each time a new player joins the game
        val playersIncludingServer = _players.value.let {
            val tmpList = it.toMutableList()
            tmpList.add(_currentPlayer.value!!)
            tmpList.toList()
        }
        sendUtf8LineToAllPlayers { player ->
            gson.toJson(playersIncludingServer.filter { it != player })
        }
    }

    private fun handleStartGameState() {
        if (!isFirstRound) return
        val currentPlayerColor =
            PlayerColors.values().filter { it !in selectedPlayerColors }.random()
        selectedPlayerColors.add(currentPlayerColor)
        _currentPlayer.value = Player(
            _currentPlayer.value!!.name, currentPlayerColor.argb.toString()
        )
    }
    // endregion

    // region UI-related functions and properties
    private val _isDisplayCategoryAndWordConfirmationSent = MutableStateFlow(false)
    val isDisplayCategoryAndWordConfirmationSent =
        _isDisplayCategoryAndWordConfirmationSent.asStateFlow()

    val onConfirmClick = fun() {
        _gameState.value = GameState.GetPlayerReadCategoryAndWordConfirmation(10)
        _isDisplayCategoryAndWordConfirmationSent.value = true
    }

    private val _wordDialogVisibilityState = mutableStateOf(false)
    val wordDialogVisibilityState: State<Boolean> = _wordDialogVisibilityState

    val onShowWordClick = {
        _wordDialogVisibilityState.value = true
    }

    val onWordDialogOkClicked = {
        _wordDialogVisibilityState.value = false
    }


    private val _isQuestionDone = MutableStateFlow(false)
    val isQuestionDone = _isQuestionDone.asStateFlow()

    val onDoneClick = fun() {
        // TODO: This should be a method you call from StateManager
        _isQuestionDone.value = true
        if (!gameData.isHost!!) {
            return
        }
        val currentGameState = _gameState.value as GameState.AskQuestion
        if (currentGameState.isLastQuestion) {
            // Handles if the last question was asked by the host
            Log.d(TAG, "Asking extra questions...")
            _gameState.value = GameState.AskExtraQuestions
        } else {
            Log.d(TAG, "Asking another question...")
            _gameState.value = GameState.AskQuestion(
                askingPairs[currentAskingPairIndex].first,
                askingPairs[currentAskingPairIndex].second,
                (askingPairs[currentAskingPairIndex].first == _currentPlayer.value),
                currentAskingPairIndex == askingPairs.lastIndex
            )
        }
    }

    val onRestartQuestionsClick = {
        // TODO: This should be a method you call from StateManager
        _isQuestionDone.value = false
        val playersIncludingServer = Server.players.values.toMutableList()
        playersIncludingServer.add(_currentPlayer.value!!)
        askingPairs = generateAllAskingCombinations(playersIncludingServer)
        currentAskingPairIndex = 0
        _gameState.value = GameState.AskQuestion(
            askingPairs[currentAskingPairIndex].first,
            askingPairs[currentAskingPairIndex].second,
            (askingPairs[currentAskingPairIndex].first == _currentPlayer.value),
            currentAskingPairIndex == askingPairs.lastIndex,
            true
        )
    }

    val onStartVoteClick = {
        _gameState.value = GameState.StartVote
    }

    private val _votedPlayer = mutableStateOf<Player?>(null)
    val votedPlayer: State<Player?> = _votedPlayer
    val onVoteForPlayer = { player: Player ->
        _votedPlayer.value = player
    }

    private val _isVoteConfirmed = MutableStateFlow(false)
    val isVoteConfirmed = _isVoteConfirmed.asStateFlow()

    val onConfirmVoteClick = {
        _isVoteConfirmed.value = true
        if (gameData.isHost!!) {
            _gameState.value = GameState.GetPlayerVote(
                _currentPlayer.value!!, votedPlayer.value!!
            )
        }
    }

    val onShowScoreClick = {
        _gameState.value = GameState.ShowScoreboard
    }

    val onEndGameClick = {
        _gameState.value = GameState.Replay(false)
    }

    val onReplayClick = {
        _gameState.value = GameState.Replay(true)
    }

    fun onJoinGamePressed(gameCode: String) {
        gameData.gameCode = gameCode
    }

    // endregion
    init {
        val defaultPlayerName = ""
        val savedPlayerName =
            sharedPreferences.getString(PLAYER_NAME_SHARED_PREF_KEY, defaultPlayerName)!!
        if (savedPlayerName != defaultPlayerName) {
            _currentPlayer.value = Player(savedPlayerName, GameConstants.DEFAULT_PLAYER_COLOR)
        }
    }
    // region Server messages handling

    suspend fun handleServerMessages(
        connection: Connection, hasFoundGame: MutableStateFlow<Boolean>
    ) {
        val gson = Gson()
        if (!Client.replay) {
            handleClientSideGameInitialization(connection, gson, hasFoundGame)
        } else {
            // TODO: This is to trigger hasFoundGame to true for now
            readLastPlayerFlagFromServer(connection, gson)
        }
        val categoryAndWordPair = readCategoryAndWordFromServer(connection, gson)
        categoryOrdinal = categoryAndWordPair.first
        wordResId = categoryAndWordPair.second
        isImposter = wordResId == -1
        _gameState.value = GameState.DisplayCategoryAndWord(categoryOrdinal, wordResId)
        _hasJoinedGame.value = true
        isDisplayCategoryAndWordConfirmationSent.first { it }
        sendCategoryAndWordConfirmationToServer(connection, gson)
        handleClientSideQuestionRoundsMessages(connection, gson)
        readStartVoteMessageFromServer(connection, gson)
        _gameState.value = GameState.StartVote
        sendVoteToServer(connection, gson)
        Log.d(TAG, "Reading 'end vote' messages from server.")
        imposterPlayer = readImposterPlayerFromServer(connection, gson)
        _votedPlayers = readVotingResultsFromServer(connection)
        _gameState.value = GameState.EndVote(_votedPlayers.maxBy { it.value }.key)
        _playerScores = readPlayerScoresFromServer(connection)
        Client.replay = readReplayFlagFromServer(connection, gson)
        // This is called regardless to end the game
        replayGame()
        _gameState.value = GameState.Replay(Client.replay)
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

    private suspend fun readVotingResultsFromServer(connection: Connection): MutableMap<Player, Int> {
        val votesType = object : TypeToken<Map<Player, Int>>() {}.type
        val json = connection.input.readUTF8Line()
        // This it to make key parsed as an object not string
        val complexGson = GsonBuilder().enableComplexMapKeySerialization().create()
        val votedPlayers =
            complexGson.fromJson<Map<Player, Int>>(json, votesType) as MutableMap<Player, Int>
        Log.d(TAG, "Voting results: $_votedPlayers")
        return votedPlayers
    }

    private suspend fun readImposterPlayerFromServer(connection: Connection, gson: Gson): Player {
        val json = connection.input.readUTF8Line()
        val imposterPlayer = gson.fromJson(json, Player::class.java)
        Log.d(TAG, "Imposter: $imposterPlayer")
        return imposterPlayer
    }

    private suspend fun sendVoteToServer(connection: Connection, gson: Gson) {
        isVoteConfirmed.first { it }
        Log.d(TAG, "Voted Player: ${votedPlayer.value}")
        Log.d(TAG, "Sending vote to server.")
        connection.output.writeLineUtf8(gson.toJson(votedPlayer.value))
    }

    private suspend fun readStartVoteMessageFromServer(connection: Connection, gson: Gson) {
        // TODO: This should be removed once you receive the list of players from server at the start
        //  of the game
        Log.d(TAG, "Reading 'start vote' message from server...")
        val json: String? = connection.input.readUTF8Line()
        val collectionType = object : TypeToken<Collection<Player>>() {}.type
        _players.value = gson.fromJson(json, collectionType)
        Log.d(TAG, "Players to vote for: ${players.value}")
    }

    private suspend fun handleClientSideQuestionRoundsMessages(connection: Connection, gson: Gson) {
        do {
            handleClientSideQuestionsRoundMessages(connection, gson)
            _gameState.value = GameState.AskExtraQuestions
            val isAnotherQuestionsRound = readExtraQuestionsRoundFlagFromServer(connection, gson)
            // Avoid skipping the current player's turn to ask a question in the next round
            _isQuestionDone.value = false
        } while (isAnotherQuestionsRound)
    }

    private suspend fun readExtraQuestionsRoundFlagFromServer(
        connection: Connection,
        gson: Gson
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
            _gameState.value = askQuestionState
            // TODO: This is to determine if this is the last question or not
            //      which is already sent by the server. This should be changed server side and
            //      then removed from here. It makes no sense since the Server sent the last question
            //      flag in the state it already sent, so why send it back to the server?
            // This is used to tell the server that the client has finished asking the question
            if (askQuestionState.isAsking) {
                _isQuestionDone.first { it }
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
        connection: Connection,
        gson: Gson
    ) {
        Log.d(TAG, "Sending confirm reading category and word message to server")
        connection.output.writeStringUtf8(gson.toJson(true) + '\n')
    }

    private suspend fun readCategoryAndWordFromServer(
        connection: Connection,
        gson: Gson
    ): Pair<Int, Int> {
        Log.d(TAG, "Reading 'category and word' message from server...")
        val type = object : TypeToken<Map<String, String>>() {}.type
        val json = connection.input.readUTF8Line()
        val categoryAndWord = gson.fromJson<Map<String, String>>(json, type)
        Log.d(
            TAG,
            "Category: ${Categories.values()[categoryAndWord["category"]!!.toInt()]}, Word: ${categoryAndWord["word"] ?: "IMPOSTER"}"
        )
        categoryOrdinal = categoryAndWord["category"]!!.toInt()
        wordResId = categoryAndWord["word"]?.toInt() ?: -1
        return Pair(categoryOrdinal, wordResId)
    }

    private suspend fun handleClientSideGameInitialization(
        connection: Connection, gson: Gson, hasFoundGame: MutableStateFlow<Boolean>
    ) {
        sendPlayerNameToServer(connection, gson)
        val playerColor: String = readPlayerColorFromServer(connection, gson)
        _currentPlayer.value = currentPlayer.value!!.copy(color = playerColor)
        readPlayersListUpdatesFromServer(connection, gson, hasFoundGame)
    }

    private suspend fun readPlayersListUpdatesFromServer(
        connection: Connection, gson: Gson, hasFoundGame: MutableStateFlow<Boolean>
    ) {
        var isLastPlayer = false
        while (!isLastPlayer) {
            // Read playersList once from server and go to lobby
            readPlayersListUpdateFromServer(connection, gson, hasFoundGame)
            hasFoundGame.value = if (!hasFoundGame.value) true else hasFoundGame.value
            // ------   End of reading players list
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
        connection: Connection, gson: Gson, hasFoundGame: MutableStateFlow<Boolean>
    ) {
        Log.d(TAG, "Reading 'players' message from server...")
        val collectionType = object : TypeToken<Collection<Player>>() {}.type
        val jsonPlayers = connection.input.readUTF8Line()
        _players.value = gson.fromJson(jsonPlayers, collectionType)
        Log.d(TAG, "Players: ${players.value}")
    }

    private suspend fun readPlayerColorFromServer(connection: Connection, gson: Gson): String {
        Log.d(TAG, "Reading 'player color' message from server...")
        val json = connection.input.readUTF8Line()
        return gson.fromJson(json, String::class.java)
    }

    private suspend fun sendPlayerNameToServer(connection: Connection, gson: Gson) {
        Log.d(TAG, "Sending player name to server.")
        connection.output.writeLineUtf8(gson.toJson(_currentPlayer.value!!.name))
    }
    // endregion

    // region Client messages handling
    private suspend fun handleClientMessages(connection: Connection) {
        Log.d(TAG, "Waiting for client message...")
        val json = connection.input.readUTF8Line()
        Log.d(TAG, "Received from client: $json")
        Log.d(TAG, "Current game state: ${_gameState.value}")
        if (json == null) {
            throw IllegalArgumentException("Received null string from client.")
        }
        val gson = Gson()
        handleStateOnMessageReceived(json, connection, gson)
    }

    private fun handleStateOnMessageReceived(json: String, connection: Connection, gson: Gson) {
        when (_gameState.value) {
            GameState.StartGame, is GameState.GetPlayerInfo -> {
                val playerName = gson.fromJson(json, String::class.java)
                _gameState.value = GameState.GetPlayerInfo(playerName, connection)
            }

            is GameState.DisplayCategoryAndWord, is GameState.GetPlayerReadCategoryAndWordConfirmation -> {
                val isConfirmed = gson.fromJson(json, Boolean::class.java)
                Log.d(TAG, "isConfirmed: $isConfirmed")
                // TODO: Find a way to count the number of confirmations in the GameState itself
                _gameState.value = GameState.GetPlayerReadCategoryAndWordConfirmation(0)
            }

            // This is sent from the player asking, confirming to end question
            is GameState.AskQuestion -> {
                val isLastQuestionConfirmation = gson.fromJson(json, Boolean::class.java)
                Log.d(TAG, "isLastQuestion = $isLastQuestionConfirmation received.")
                if (isLastQuestionConfirmation) {
                    Log.d(TAG, "Asking extra questions...")
                    _gameState.value = GameState.AskExtraQuestions
                } else {
                    Log.d(TAG, "Asking another question...")
                    _gameState.value = GameState.AskQuestion(
                        askingPairs[currentAskingPairIndex].first,
                        askingPairs[currentAskingPairIndex].second,
                        (askingPairs[currentAskingPairIndex].first == _currentPlayer.value),
                        currentAskingPairIndex == askingPairs.lastIndex
                    )
                }
            }

            GameState.AskExtraQuestions -> {
                Log.wtf(TAG, "Received a message while in GameState.AskExtraQuestions.")
            }

            GameState.StartVote, is GameState.GetPlayerVote -> {
                val votedPlayer = gson.fromJson(json, Player::class.java)
                _gameState.value =
                    GameState.GetPlayerVote(Server.players[connection]!!, votedPlayer)
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

    private fun generateGameCode(): String {
        val allowedChars = ('A'..'Z') + ('0'..'9')
        return (1..GameConstants.CODE_LENGTH).map { allowedChars.random() }.joinToString("")
    }

    fun replayGame() {
        resetRoundParameters()
        resetUIStates()
    }

    private fun resetRoundParameters() {
        _gameState.value = GameState.StartGame
        numberOfReadCategoryAndWordConfirmations = 0
        askingPairs = emptyList()
        currentAskingPairIndex = 0
        numberOfVoters = 0
        _votedPlayers = mutableMapOf()
        playerVotes.clear()
        isImposter = null
    }

    private fun resetGameParameters() {
        if (gameData.isHost!!) {
            selectedPlayerColors.clear()
            isFirstRound = true
            Server.players.clear()
        }
        gameData.isHost = false
        _playerScores.clear()
        _currentPlayer.value =
            currentPlayer.value!!.copy(color = GameConstants.DEFAULT_PLAYER_COLOR)
        _players.value = emptyList()
        mServiceName = BASE_SERVICE_NAME
    }

    private fun resetUIStates() {
        // CreateGame Screen
        //TODO: Maybe this shouldn't be reset
        _isGameCreated.value = false
        // JoinGame Screen
        //TODO: Maybe this shouldn't be reset
        _hasJoinedGame.value = false
        // CategoryAndWord Screen
        _isDisplayCategoryAndWordConfirmationSent.value = false
        // Question Screen
        _isQuestionDone.value = false
        // Voting Screen
        _votedPlayer.value = null
        _isVoteConfirmed.value = false
    }

    fun updateCurrentPlayer(player: Player) {
        _currentPlayer.value = player
        with(sharedPreferences.edit()) {
            putString(PLAYER_NAME_SHARED_PREF_KEY, player.name)
            apply()
        }
    }

    fun setClientJob(job: Job) {
        clientJob = job
    }

    fun endGame() {
        if (gameData.isHost!!) {
            serverJob.cancel()
            gameStateCollectorJob.cancel()
            nsdManager.unregisterService(registrationListener)
        } else {
            clientJob.cancel()
        }
        replayGame()
        resetGameParameters()
    }

    fun chooseWordRandomly(chosenCategory: Categories) {
        choosePlayerColor()
        _gameState.value = GameState.DisplayCategoryAndWord(
            chosenCategory.ordinal, chosenCategory.wordResourceIds.random()
        )
    }

    private fun choosePlayerColor() {
        if (!isFirstRound) return
        val currentPlayerColor =
            PlayerColors.values().filter { it !in selectedPlayerColors }.random()
        selectedPlayerColors.add(currentPlayerColor)
        _currentPlayer.value = Player(
            _currentPlayer.value!!.name, currentPlayerColor.argb.toString()
        )
    }

    // region Message utility functions
    private suspend fun sendUtf8LineToAllPlayers(messageFun: (Player) -> String) {
        for ((clientConnection, player) in Server.players) {
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