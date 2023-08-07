package com.example.nsddemo.ui

import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.text.format.Formatter
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nsddemo.Debugging.TAG
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
import io.ktor.utils.io.readUTF8Line
import io.ktor.utils.io.writeStringUtf8
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class GameViewModel(val nsdManager: NsdManager, val wifiManager: WifiManager) : ViewModel() {
    private var mServiceName: String = BASE_SERVICE_NAME
    private val _isGameCreated = mutableStateOf(false)
    val isGameCreated: State<Boolean> = _isGameCreated
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

    lateinit var gameCode: String
        private set
    private val _hasJoinedGame = mutableStateOf(false)
    val hasJoinedGame: State<Boolean> = _hasJoinedGame

    private val _gameState: MutableStateFlow<GameState> = MutableStateFlow(GameState.StartGame)
    val gameState = _gameState.asStateFlow()
    var isHost = false
        private set
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
            return _playerScores.toMap()
        }

    //TODO: It should be sent to the clients as response to their player name message
    private val _currentPlayer: MutableState<Player?> = mutableStateOf(null)
    val currentPlayer: State<Player?> = _currentPlayer
    private lateinit var imposterPlayer: Player
    var isImposter: Boolean? = null
        private set
    private val _players = mutableStateOf(emptyList<Player>())
    val players: State<List<Player>> = _players
    private val selectedPlayerColors = mutableListOf<PlayerColors>()
    private lateinit var clientJob: Job
    private lateinit var serverJob: Job
    val onRegisterServiceClick = {
        viewModelScope.launch(Dispatchers.IO) {
            val gson = Gson()
            gameState.collect {
                when (val currentGameState = gameState.value) {
                    GameState.StartGame -> {}

                    is GameState.GetPlayerInfo -> {
                        val playerColor =
                            PlayerColors.values().filter { it !in selectedPlayerColors }.random()
                        selectedPlayerColors.add(playerColor)
                        Server.players[currentGameState.connection] =
                            Player(currentGameState.name, playerColor.argb.toString())
                        _players.value = Server.players.values.toList()
                    }

                    // This game state will happen when the user which acts as the server
                    // presses a button like "start game"
                    is GameState.DisplayCategoryAndWord -> {
                        val currentPlayerColor =
                            PlayerColors.values().filter { it !in selectedPlayerColors }.random()
                        selectedPlayerColors.add(currentPlayerColor)
                        _currentPlayer.value =
                            Player(_currentPlayer.value!!.name, currentPlayerColor.argb.toString())
                        val playersIncludingServer = Server.players.values.toMutableList()
                        playersIncludingServer.add(_currentPlayer.value!!)
                        askingPairs = generateAllAskingCombinations(playersIncludingServer)
                        imposterPlayer = playersIncludingServer.random()
                        isImposter = (imposterPlayer == _currentPlayer.value)
                        val category = currentGameState.category
                        val word = currentGameState.word
                        sendUtf8LineToAllPlayers { player ->
                            gson.toJson(
                                if (player == imposterPlayer) mapOf("category" to category)
                                else mapOf("category" to category, "word" to word)
                            )
                        }
                    }

                    is GameState.GetPlayerReadCategoryAndWordConfirmation -> {
                        numberOfReadCategoryAndWordConfirmations++
                        Log.d(TAG, "# of Confirmations= $numberOfReadCategoryAndWordConfirmations")
                        Log.d(TAG, "Server.players.size + 1= ${Server.players.size + 1}")
                        if (numberOfReadCategoryAndWordConfirmations == Server.players.size + 1) {
                            Log.d(
                                TAG,
                                "# of confirmations = $numberOfReadCategoryAndWordConfirmations"
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

                    is GameState.AskQuestion -> {
                        sendUtf8LineToAllPlayers { player ->
                            gson.toJson(
                                currentGameState.copy(
                                    isAsking = currentGameState.asker == player
                                )
                            )
                        }
                        currentAskingPairIndex++
                    }

                    GameState.AskExtraQuestions -> {
                        for ((clientConnection, _) in Server.players) {
                            clientConnection.output.writeStringUtf8(gson.toJson(true) + '\n')
                        }
                    }

                    // This game state will happen when the user which acts as the server
                    // presses a button like "start vote"
                    // (The screen will be like any additional questions?)
                    GameState.StartVote -> {
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

                    is GameState.GetPlayerVote -> {
                        Log.d(
                            TAG, "${currentGameState.voter} voted for ${currentGameState.voted}"
                        )
                        val votedPlayer = currentGameState.voted
                        _votedPlayers[votedPlayer] = (_votedPlayers[votedPlayer] ?: 0) + 1
                        playerVotes[currentGameState.voter] = currentGameState.voted
                        numberOfVoters++
                        Log.d(
                            TAG,
                            "numberOfVoters=$numberOfVoters, numOfPlayers=${Server.players.size + 1}"
                        )
                        if (numberOfVoters == Server.players.size + 1) {
                            Log.d(TAG, "All players voted. Ending vote...")
                            _gameState.value =
                                GameState.EndVote(_votedPlayers.maxBy { it.value }.key)
                        }
                    }

                    //This event is triggered when all players have voted
                    // Should send the votes list to all players
                    is GameState.EndVote -> {
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

                    // Triggered by user pressing a button (Should probably be sent with the votes list to
                    // be ready to be shown on screen right after to all players
                    GameState.ShowScoreboard -> {}

                    // - For now this should send a message to all clients informing them
                    // that the game will be continued.
                    // - For now also this will be determined by the server, no voting.
                    is GameState.Replay -> {
                        Log.d(TAG, "Sending replay status to all players.")
                        sendUtf8LineToAllPlayers {
                            gson.toJson(currentGameState.replay)
                        }
                    }
                }
            }
        }
        isHost = true
        gameCode = generateGameCode()
        Log.d(TAG, "gameCode: $gameCode")
        var serverIP: String? = null
        try {
            serverIP = Formatter.formatIpAddress(wifiManager.connectionInfo.ipAddress)
            Log.d(TAG, "Device IP: $serverIP")
        } catch (e: Exception) {
            Log.d(TAG, e.message.toString())
        }
        val serverPort: Int = Server.initServerSocket(serverIP!!)
        serverJob = CoroutineScope(Dispatchers.IO).launch {
            Log.d(TAG, "Started server")
            Server.run(::handleClientMessages)
        }
        Log.d(TAG, serverJob.children.toString())
        registerService(serverPort, gameCode)
    }

    val onStartVoteClick = {
        _gameState.value = GameState.StartVote
    }

    val onShowScoreClick = {
        _gameState.value = GameState.ShowScoreboard
    }
    val onReplayClick = {
        _gameState.value = GameState.Replay(true)
    }
    val onEndGameClick = {
        _gameState.value = GameState.Replay(false)
    }

    private val _isDisplayCategoryAndWordConfirmationSent = MutableStateFlow(false)
    val isDisplayCategoryAndWordConfirmationSent =
        _isDisplayCategoryAndWordConfirmationSent.asStateFlow()

    val onConfirmClick = fun() {
        _gameState.value = GameState.GetPlayerReadCategoryAndWordConfirmation(10)
        _isDisplayCategoryAndWordConfirmationSent.value = true
    }

    private val _isQuestionDone = MutableStateFlow(false)
    val isQuestionDone = _isQuestionDone.asStateFlow()

    val onDoneClick = fun() {
        _isQuestionDone.value = true
        if (!isHost) {
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

    private val _votedPlayer = mutableStateOf<Player?>(null)
    val votedPlayer: State<Player?> = _votedPlayer
    val onVoteForPlayer = { player: Player ->
        _votedPlayer.value = player
    }

    private val _isVoteConfirmed = MutableStateFlow(false)
    val isVoteConfirmed = _isVoteConfirmed.asStateFlow()

    val onConfirmVoteClick = {
        _isVoteConfirmed.value = true
        if (isHost) {
            _gameState.value = GameState.GetPlayerVote(
                _currentPlayer.value!!, votedPlayer.value!!
            )
        }
    }

    suspend fun handleServerMessages(connection: Connection) {
        val gson = Gson()
        if (!Client.replay) {
            Log.d(TAG, "Sending player name to server.")
            connection.output.writeStringUtf8(gson.toJson(_currentPlayer.value!!.name) + '\n')
        }
        Log.d(TAG, "Reading 'category and word' message from server...")
        val type = object : TypeToken<Map<String, String>>() {}.type
        var json = connection.input.readUTF8Line()
        val categoryAndWord = gson.fromJson<Map<String, String>>(json, type)
        Log.d(
            TAG,
            "Category: ${categoryAndWord["category"]!!}, Word: ${categoryAndWord["word"] ?: "IMPOSTER"}"
        )
        isImposter = categoryAndWord["word"] == null
        _gameState.value = GameState.DisplayCategoryAndWord(
            categoryAndWord["category"]!!, categoryAndWord["word"] ?: "IMPOSTER"
        )
        //TODO: This is to be moved after sending list of players in lobby to all players
        _hasJoinedGame.value = true
        isDisplayCategoryAndWordConfirmationSent.first { it }
        Log.d(TAG, "Sending confirm reading category and word message to server")
        connection.output.writeStringUtf8(gson.toJson(true) + '\n')
        do {
            Log.d(TAG, "Reading 'ask question' message from server...")
            json = connection.input.readUTF8Line()
            Log.d(TAG, "$json")
            val askQuestionState = gson.fromJson(json, GameState.AskQuestion::class.java)
            _gameState.value = askQuestionState
            if (askQuestionState.isAsking) {
                _isQuestionDone.first { it }
                Log.d(TAG, "Sending 'isLast' message to server.")
                connection.output.writeStringUtf8(gson.toJson(askQuestionState.isLastQuestion) + '\n')
            }
        } while (!(askQuestionState.isLastQuestion))
        Log.d(TAG, "Reading 'ask extra questions' message from server...")
        json = connection.input.readUTF8Line()
        val isLastQuestionDone = gson.fromJson(json, Boolean::class.java)
        Log.d(TAG, "isLastQuestionDone = $isLastQuestionDone")
        _gameState.value = GameState.AskExtraQuestions
        Log.d(TAG, "Reading 'start vote' message from server...")
        json = connection.input.readUTF8Line()
        val collectionType = object : TypeToken<Collection<Player>>() {}.type
        _players.value = gson.fromJson(json, collectionType)
        Log.d(TAG, "Players to vote for: ${players.value}")
        _gameState.value = GameState.StartVote
        isVoteConfirmed.first { it }
        Log.d(TAG, "Voted Player: ${votedPlayer.value}")
        Log.d(TAG, "Sending vote to server.")
        connection.output.writeStringUtf8(gson.toJson(votedPlayer.value) + '\n')
        Log.d(TAG, "Reading 'end vote' message from server.")
        json = connection.input.readUTF8Line()
        imposterPlayer = gson.fromJson(json, Player::class.java)
        Log.d(TAG, "Imposter: $imposterPlayer")
        val votesType = object : TypeToken<Map<Player, Int>>() {}.type
        json = connection.input.readUTF8Line()
        Log.d(TAG, json!!)
        // This it to make key parsed as an object not string
        val complexGson = GsonBuilder().enableComplexMapKeySerialization().create()
        _votedPlayers =
            complexGson.fromJson<Map<Player, Int>>(json, votesType) as MutableMap<Player, Int>
        Log.d(TAG, "Voting results: $_votedPlayers")
        _gameState.value = GameState.EndVote(_votedPlayers.maxBy { it.value }.key)
        json = connection.input.readUTF8Line()
        _playerScores =
            complexGson.fromJson<Map<Player, Int>>(json, votesType) as MutableMap<Player, Int>
        Log.d(TAG, "Scores: $_playerScores")
        Log.d(TAG, "Reading 'replay' message from server...")
        json = connection.input.readUTF8Line()
        val replay = gson.fromJson(json, Boolean::class.java)
        Log.d(TAG, "$replay")
        Client.replay = replay
        replayGame()
        _gameState.value = GameState.Replay(replay)
    }

    /**
     * Generates a [List] of [Player] pairs such that each player asks and is asked exactly once
     */
    private fun generateAllAskingCombinations(firstPlayerList: List<Player>): List<Pair<Player, Player>> {
        val secondPlayerList = firstPlayerList.toMutableList()
        val pairs = mutableListOf<Pair<Player, Player>>()

        firstPlayerList.shuffled().map { firstPlayer ->
            val firstPlayerIndex =
                secondPlayerList.indexOf(firstPlayer)
            val secondPlayer =
                secondPlayerList.filterIndexed { index, _ -> index != firstPlayerIndex }.random()
            secondPlayerList.remove(secondPlayer)
            pairs.add(firstPlayer to secondPlayer)
        }

        return pairs
    }

    private fun registerService(port: Int, gameCode: String) {
        // Create the NsdServiceInfo object, and populate it.
        val serviceInfo = NsdServiceInfo().apply {
            // The name is subject to change based on conflicts
            // with other services advertised on the same network.
            serviceName = if (isHost) mServiceName + "_$gameCode" else mServiceName
            serviceType = SERVICE_TYPE
            setPort(port)
        }
        Log.d(TAG, "Created serviceInfo")
        try {
            nsdManager.apply {
                // TODO: Handle registering multiple times for service and for server (Look more into
                //  coroutines and stuff)
                this.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
            }
        } catch (E: Exception) {
            Log.e(TAG, E.message.toString())
        } finally {

        }
        Log.d(TAG, "Created nsdManager")
    }

    private suspend fun handleClientMessages(connection: Connection) {
        Log.d(TAG, "Waiting for client message...")
        val json = connection.input.readUTF8Line()
        Log.d(TAG, "Received from client: $json")
        Log.d(TAG, "Current game state: ${_gameState.value}")
        if (json == null) {
            throw IllegalArgumentException("Received null string from client.")
        }
        val gson = Gson()
//        val type = object : TypeToken<Map<String?, String?>?>() {}.type
//        val myMap: Map<String, String>? = gson.fromJson<Map<String, String>>(json, type)
//        Log.d(TAG, "JSON: $myMap")
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

            GameState.StartVote -> {
                val votedPlayer = gson.fromJson(json, Player::class.java)
                _gameState.value =
                    GameState.GetPlayerVote(Server.players[connection]!!, votedPlayer)
            }

            is GameState.GetPlayerVote -> {
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

    private fun generateGameCode(): String {
        val allowedChars = ('A'..'Z') + ('0'..'9')
        return (1..CODE_LENGTH).map { allowedChars.random() }.joinToString("")
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

    private fun resetGameParameters() {
        _gameState.value = GameState.StartGame
        numberOfReadCategoryAndWordConfirmations = 0
        askingPairs = emptyList()
        currentAskingPairIndex = 0
        numberOfVoters = 0
        _votedPlayers = mutableMapOf()
        playerVotes.clear()
        isImposter = null
    }

    fun replayGame() {
        resetGameParameters()
        resetUIStates()
    }
    fun updateCurrentPlayer(player: Player){
        _currentPlayer.value = player
    }

    fun updateGameState(gameState: GameState) {
        _gameState.value = gameState
    }
    fun setClientJob(job: Job) {
        clientJob = job
    }
    private fun String.appendNewLine(): String {
        return this + '\n'
    }

    private suspend fun sendUtf8LineToAllPlayers(messageFun: (Player) -> String) {
        for ((clientConnection, player) in Server.players) {
            clientConnection.output.writeStringUtf8(
                messageFun(player).appendNewLine()
            )
        }
    }

    companion object {
        private const val CODE_LENGTH = 4
        private const val PLAYER_SCORE_INCREMENT = 100

    }
}