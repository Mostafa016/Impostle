package com.example.nsddemo.ui

import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.text.format.Formatter
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import com.example.nsddemo.Client
import com.example.nsddemo.Debugging.TAG
import com.example.nsddemo.GameState
import com.example.nsddemo.Player
import com.example.nsddemo.Server
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import io.ktor.network.sockets.Connection
import io.ktor.utils.io.readUTF8Line
import io.ktor.utils.io.writeStringUtf8
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.net.InetAddress

class TestViewModel(val nsdManager: NsdManager, val wifiManager: WifiManager) : ViewModel() {
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

    private lateinit var mService: NsdServiceInfo
    lateinit var gameCode: String
        private set
    private val _hasJoinedGame = mutableStateOf(false)
    val hasJoinedGame: State<Boolean> = _hasJoinedGame
    private val discoveryListener = object : NsdManager.DiscoveryListener {
        // Called as soon as service discovery begins.
        override fun onDiscoveryStarted(regType: String) {
            Log.d(TAG, "Service discovery started")
        }

        override fun onServiceFound(service: NsdServiceInfo) {
            // A service was found! Do something with it.
            Log.d(TAG, "Service discovery success: $service")
            when {
                service.serviceType != SERVICE_TYPE -> // Service type is the string containing the protocol and
                    // transport layer for this service.
                    Log.d(TAG, "Unknown Service Type: ${service.serviceType}")

                service.serviceName == mServiceName -> // The name of the service tells the user what they'd be
                    // connecting to. It could be "Bob's Chat App".
                    Log.d(TAG, "Same machine: $mServiceName")

                service.serviceName.contains(BASE_SERVICE_NAME) -> {
                    // Not host
                    if (service.serviceName.split("_").size != 2) {
                        Log.d(TAG, "${service.serviceName} does not belong to a host")
                        return
                    }
                    // Not the host of the lobby I want to join
                    val serviceGameCode = service.serviceName.split("_")[1].lowercase()
                    if (serviceGameCode != gameCode) {
                        Log.d(
                            TAG,
                            "${service.serviceName} is not the host of the game I want to join with code $gameCode"
                        )
                        return
                    }
                    nsdManager.resolveService(
                        service, resolveListener
                    )
                    Log.d(TAG, "Found app service: ${service.serviceName}")
                }
            }
        }

        private val resolveListener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                // Called when the resolve fails. Use the error code to debug.
                Log.e(TAG, "Resolve failed: $errorCode")
            }

            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                Log.e(TAG, "Resolve Succeeded. $serviceInfo")

                if (serviceInfo.serviceName == mServiceName) {
                    Log.d(TAG, "Same IP.")
                    return
                }
                // Host of another game
                if (serviceInfo.serviceName.split("_")[1].lowercase() != gameCode) {
                    Log.d(
                        TAG, "${serviceInfo.serviceName} Host of another game $gameCode"
                    )
                    return
                }
                mService = serviceInfo
                // Save port and ip address for communication with sockets
                val port: Int = serviceInfo.port
                val host: InetAddress = serviceInfo.host
                Log.d(TAG, "Client connecting to server with: ")
                Log.d(TAG, "Port: $port")
                Log.d(TAG, "Host: $host")
                CoroutineScope(Dispatchers.IO).launch {
                    Log.d(TAG, "Started client")
                    Log.d(TAG, "Address: ${host.hostAddress!!} Port: $port")
                    Client.run(host.hostAddress!!, port, ::handleServerMessages)
                }
                //_hasJoinedGame.value = true
            }
        }

        override fun onServiceLost(service: NsdServiceInfo) {
            // When the network service is no longer available.
            // Internal bookkeeping code goes here.
            // TODO: Figure out when this happens as it causes duplicate players in server
            // when it happens from client
            Log.e(TAG, "Service lost: $service")
        }

        override fun onDiscoveryStopped(serviceType: String) {
            Log.i(TAG, "Discovery stopped: $serviceType")
        }

        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e(TAG, "Discovery failed: Error code:$errorCode")
            nsdManager.stopServiceDiscovery(this)
        }

        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e(TAG, "Discovery failed: Error code:$errorCode")
            nsdManager.stopServiceDiscovery(this)
        }
    }

    private val _gameState: MutableStateFlow<GameState> = MutableStateFlow(GameState.StartGame)
    val gameState = _gameState.asStateFlow()
    var isHost = false
        private set
    private var LOCK = Any()
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

    //TODO: Define this using input from user and random color
    // Also random colors for all players (This should be renamed to current player)
    // It should be sent to the clients as response to their player name message
    private val _currentPlayer: MutableState<Player?> = mutableStateOf(null)
    val currentPlayer: State<Player?> = _currentPlayer
    private lateinit var imposterPlayer: Player
    var isImposter: Boolean? = null
        private set
    private val _players = mutableStateOf(emptyList<Player>())
    val players: State<List<Player>> = _players
    private val selectedPlayerColors = mutableListOf<PlayerColors>()
    private lateinit var chosenCategory: Categories
    val onRegisterServiceClick = {
        CoroutineScope(Dispatchers.IO).launch {
            gameState.collect {
                val gson = Gson()
                when (val currentGameState = gameState.value) {
                    GameState.StartGame -> {}

                    is GameState.GetPlayerInfo -> {
                        val playerColor =
                            PlayerColors.values().filter { it !in selectedPlayerColors }.random()
                        selectedPlayerColors.add(playerColor)
                        synchronized(LOCK) {
                            Server.players[currentGameState.connection] =
                                Player(currentGameState.name, playerColor.argb.toString())
                            _players.value = Server.players.values.toList()
                        }
                    }

                    // This game state will happen when the user which acts as the server
                    // presses a button like "start game"
                    is GameState.DisplayCategoryAndWord -> {
                        val playersIncludingServer = Server.players.values.toMutableList()
                        playersIncludingServer.add(_currentPlayer.value!!)
                        askingPairs = generateAllAskingCombinations(playersIncludingServer)
                        // Randomly choose imposter from list of players
                        imposterPlayer = playersIncludingServer.random()
                        isImposter = (imposterPlayer == _currentPlayer.value)
                        //TODO: Function to choose category and word
                        val category = currentGameState.category
                        val word = currentGameState.word
                        // Send category and word to all players except for imposter
                        // which only receives category
                        for ((clientConnection, player) in Server.players) {
                            clientConnection.output.writeStringUtf8(
                                gson.toJson(
                                    if (player == imposterPlayer) mapOf("category" to category)
                                    else mapOf("category" to category, "word" to word)
                                ) + '\n'
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
                        for ((clientConnection, player) in Server.players) {
                            clientConnection.output.writeStringUtf8(
                                gson.toJson(
                                    currentGameState.copy(
                                        isAsking = currentGameState.asker == player
                                    )
                                ) + '\n'
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
                        for ((clientConnection, player) in Server.players) {
                            //TODO: Add extension function that appends '\n' to string
                            clientConnection.output.writeStringUtf8(gson.toJson(allPlayers.filter { it != player }) + '\n')
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

                    is GameState.EndVote -> {
                        //This event is triggered when all players have voted
                        // Should send the votes list to all players
                        val complexGson = GsonBuilder().enableComplexMapKeySerialization().create()
                        for ((voter, voted) in playerVotes) {
                            _playerScores[voter] = (_playerScores[voter]
                                ?: 0) + if (voted == imposterPlayer) PLAYER_SCORE_INCREMENT else 0
                        }
                        for ((clientConnection, _) in Server.players) {
                            clientConnection.output.writeStringUtf8(
                                gson.toJson(
                                    imposterPlayer
                                ) + '\n'
                            )
                            clientConnection.output.writeStringUtf8(
                                complexGson.toJson(
                                    _votedPlayers
                                ) + '\n'
                            )
                            clientConnection.output.writeStringUtf8(
                                complexGson.toJson(
                                    _playerScores
                                ) + '\n'
                            )
                        }
                    }

                    // Triggered by user pressing a button (Should probably be sent with the votes list to
                    // be ready to be shown on screen right after to all players
                    GameState.ShowScoreboard -> {}

                    is GameState.Replay -> {
                        // - For now this should send a message to all clients informing them
                        // that the game will be continued.
                        // - For now also this will be determined by the server, no voting.
                        Log.d(TAG, "Sending replay status to all players.")
                        for ((clientConnection, _) in Server.players) {
                            clientConnection.output.writeStringUtf8(
                                gson.toJson(
                                    currentGameState.replay
                                ) + '\n'
                            )
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
        val tmp = CoroutineScope(Dispatchers.IO).launch {
            Log.d(TAG, "Started server")
            Server.run(::handleClientMessages)
        }
        Log.d(TAG, tmp.children.toString())
        registerService(serverPort, gameCode)
    }

    val onSendCategoryAndWordClick = {
        _gameState.value = GameState.DisplayCategoryAndWord(
            chosenCategory.toString(),
            chosenCategory.words.random()
        )
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

    private val _gameCodeTextFieldState = mutableStateOf("")
    val gameCodeTextFieldState: State<String> = _gameCodeTextFieldState
    val onGameCodeTextFieldValueChange = { text: String ->
        _gameCodeTextFieldState.value = text
    }
    val onDiscoverAndResolveServicesClick = {
        // TODO: Pass lobby "code" to it so it uniquely identifies the host
        gameCode = gameCodeTextFieldState.value.lowercase()
        Log.d(TAG, "Game Code: $gameCode")
        discoverServices()
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

    private val _playerNameDialogVisibilityState = mutableStateOf(false)
    val playerNameDialogVisibilityState: State<Boolean> = _playerNameDialogVisibilityState

    val showPlayerNameDialog = {
        _playerNameDialogVisibilityState.value = true
    }

    val onPlayerNameClick = {
        _playerNameDialogVisibilityState.value = true
    }

    private val _playerNameTextFieldState = mutableStateOf("")
    val playerNameTextFieldState: State<String> = _playerNameTextFieldState

    val onPlayerNameChange = fun(playerName: String) {
        _playerNameTextFieldState.value = playerName
    }
    val onSavePlayerNameClick = {
        val playerName = playerNameTextFieldState.value
        if (playerName.matches(Regex("^[A-Za-z_]+$"))) {
            _currentPlayer.value = Player(playerName, "FFFFFFFF")
            _playerNameDialogVisibilityState.value = false
        }
    }
    val onCancelPlayerNameClick = {
        _playerNameDialogVisibilityState.value = false
    }

    private suspend fun handleServerMessages(connection: Connection) {
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
//        val chosenPlayer = playersList.random()
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

    private fun generateAllAskingCombinations(firstPlayerList: List<Player>): List<Pair<Player, Player>> {
        val secondPlayerList = firstPlayerList.toMutableList()
        val pairs = mutableListOf<Pair<Player, Player>>()

        firstPlayerList.shuffled().map { firstPlayer ->
            val firstPlayerIndex =
                secondPlayerList.indexOf(firstPlayer) // To remove only one of the duplicate names
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

    private fun discoverServices() {
        Log.d(TAG, "Before discover services 1")
        try {
//            nsdManager = (getSystemService(Context.NSD_SERVICE) as NsdManager)
            Log.d(TAG, nsdManager.toString())
        } catch (e: Exception) {
            Log.d(TAG, e.message.toString())
        }
        Log.d(TAG, "Before discover services 2")
        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }

    //TODO: All lists should be in ViewModel or Activity for now but moved to ViewModel later.
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
                if (_currentPlayer.value?.color?.lowercase() == Color.Black.value.toString(16)) {
                    val playerColor =
                        PlayerColors.values().filter { it !in selectedPlayerColors }.random()
                    selectedPlayerColors.add(playerColor)
                    _currentPlayer.value =
                        Player(_currentPlayer.value!!.name, playerColor.argb.toString())
                }
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
                //TODO: - Add AskExtraQuestions game state and transfer to that state when
                // isLastQuestionConfirmation is true
                // - in collect send extraquestions message to all players
                // - leave the isdone or confirmed thing to account for last player being
                // asked is the host but replace the state with extraquestions in the questionscreen
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

    fun chooseCategory(category: Categories) {
        chosenCategory = category
    }

    companion object {
        private const val BASE_SERVICE_NAME = "NsdChat"
        private const val SERVICE_TYPE = "_nsdchat._tcp."
        private const val CODE_LENGTH = 4
        private const val PLAYER_SCORE_INCREMENT = 100
    }
}