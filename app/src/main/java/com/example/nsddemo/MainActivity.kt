package com.example.nsddemo

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Bundle
import android.text.format.Formatter
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.nsddemo.Debugging.TAG
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.ktor.network.sockets.Connection
import io.ktor.utils.io.readUTF8Line
import io.ktor.utils.io.writeStringUtf8
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.InetAddress


class MainActivity : AppCompatActivity() {

    private var mServiceName: String = BASE_SERVICE_NAME

    private lateinit var nsdManager: NsdManager

    private lateinit var mService: NsdServiceInfo

    private lateinit var gameCode: String

    private var isHost = false

    private var LOCK = Any()

    private val _gameState: MutableStateFlow<GameState> = MutableStateFlow(GameState.StartGame)
    val gameState = _gameState.asStateFlow()

    private lateinit var askingPairs: List<Pair<Player, Player>>

    private var currentAskingPairIndex = 0

    private var numberOfVoters = 0

    private val votedPlayers: MutableMap<Player, Int> =
        emptyMap<Player, Int>() as MutableMap<Player, Int>

    //TODO: Define this using input from user and random color
    private val serverPlayer = Player("Mostafa", "00FF00")

    private val registrationListener = object : NsdManager.RegistrationListener {


        override fun onServiceRegistered(NsdServiceInfo: NsdServiceInfo) {
            // Save the service name. Android may have changed it in order to
            // resolve a conflict, so update the name you initially requested
            // with the name Android actually used.
            mServiceName = NsdServiceInfo.serviceName
            Log.d(TAG, "Service address: ${NsdServiceInfo.host} ${NsdServiceInfo.port}")
            Log.d(TAG, "onServiceRegistered: serviceName = $mServiceName")
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

    // Instantiate a new DiscoveryListener
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
                    Log.d(TAG, "${serviceInfo.serviceName} Host of another game $gameCode")
                    return
                }
                mService = serviceInfo
                // TODO: Save port and ip address for communication with sockets
                val port: Int = serviceInfo.port
                val host: InetAddress = serviceInfo.host
                Log.d(TAG, "Client connecting to server with: ")
                Log.d(TAG, "Port: $port")
                Log.d(TAG, "Host: $host")
                CoroutineScope(Dispatchers.IO).launch {
                    Log.d(TAG, "Started client")
                    Log.d(TAG, "Address: ${host.hostAddress!!} Port: $port")
                    Client.run(host.hostAddress!!, port)
                }
            }
        }

        override fun onServiceLost(service: NsdServiceInfo) {
            // When the network service is no longer available.
            // Internal bookkeeping code goes here.
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btnRegisterService).setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                gameState.collect {
                    when (gameState.value) {
                        GameState.StartGame -> TODO()
                        is GameState.GetPlayerName -> TODO()
                        is GameState.DisplayCategoryAndWord -> TODO()
                        is GameState.AskQuestion -> TODO()
                        GameState.StartVote -> TODO()
                        is GameState.GetPlayerVote -> TODO()
                        is GameState.EndVote -> TODO()
                        GameState.ShowScoreboard -> TODO()
                        GameState.Replay -> TODO()
                    }
                }
            }
            gameCode = generateGameCode()
            Log.d(TAG, "gameCode: $gameCode")
            findViewById<TextView>(R.id.tvCode).text = "Code: $gameCode"
            var serverIP: String? = null
            try {
                val wm = this.getSystemService(WIFI_SERVICE) as WifiManager
                serverIP = Formatter.formatIpAddress(wm.connectionInfo.ipAddress)
                Log.d(TAG, "Device IP: $serverIP")
            } catch (e: Exception) {
                Log.d(TAG, e.message.toString())
            }
            val serverPort: Int = Server.initServerSocket(serverIP!!)
            val tmp = CoroutineScope(Dispatchers.IO).launch {
                Log.d(TAG, "Started server")
                Server.run(::handleMessages)
            }
            Log.d(TAG, tmp.children.toString())
            registerService(serverPort, gameCode)
        }

        findViewById<Button>(R.id.btnDiscoverAndResolveService).setOnClickListener {
            // TODO: Pass lobby "code" to it so it uniquely identifies the host
            gameCode = findViewById<EditText>(R.id.etGameCode).text.toString().lowercase()
            Log.d(TAG, "Button Clicked")
            discoverServices()
        }

        findViewById<Switch>(R.id.switchIsHost).setOnCheckedChangeListener { compoundButton, isChecked ->
            isHost = isChecked
        }

    }

    override fun onPause() {
        //tearDownNsdService()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        // registerService(connection.socket.localAddress.toJavaAddress().port, gameCode)
        //discoverServices()
    }

    override fun onDestroy() {
        //tearDownNsdService()
        // connection.socket.close()
        super.onDestroy()
    }

    private fun generateGameCode(): String {
        val allowedChars = ('A'..'Z') + ('0'..'9')
        return (1..CODE_LENGTH).map { allowedChars.random() }.joinToString("")
    }

    // NsdHelper's tearDown method
    private fun tearDownNsdService() {
        nsdManager.apply {
            unregisterService(registrationListener)
            stopServiceDiscovery(discoveryListener)
        }
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
            nsdManager = (getSystemService(Context.NSD_SERVICE) as NsdManager).apply {
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
            nsdManager = (getSystemService(Context.NSD_SERVICE) as NsdManager)
            Log.d(TAG, nsdManager.toString())
        } catch (e: Exception) {
            Log.d(TAG, e.message.toString())
        }
        Log.d(TAG, "Before discover services 2")
        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }


    private fun generateAllAskingCombinations(firstPlayerList: List<Player>): List<Pair<Player, Player>> {
        val secondPlayerList = firstPlayerList.toMutableList()
        val pairs = mutableListOf<Pair<Player, Player>>()

        firstPlayerList.shuffled().map { firstPlayer ->
            val firstPlayerIndex =
                secondPlayerList.indexOf(firstPlayer) // To remove only one of the duplicate names
            val secondPlayer =
                secondPlayerList.filterIndexed { index, _ -> index != firstPlayerIndex }
                    .random()
            secondPlayerList.remove(secondPlayer)
            pairs.add(firstPlayer to secondPlayer)
        }

        return pairs
    }

    //TODO: All lists should be in ViewModel or Activity for now but moved to ViewModel later.
    private suspend fun handleMessages(connection: Connection) {
        Log.d(TAG, "Waiting for client message...")
        val json = connection.input.readUTF8Line()
        Log.d(TAG, "Received from client: $json")
        val gson = Gson()
        val type = object : TypeToken<Map<String?, String?>?>() {}.type
        val myMap: Map<String, String>? = gson.fromJson<Map<String, String>>(json, type)
        Log.d(TAG, "JSON: $myMap")
        when (_gameState.value) {
            GameState.StartGame, is GameState.GetPlayerName -> {
                val playerName = gson.fromJson(json, String::class.java)
                synchronized(LOCK)
                {
                    Server.players.put(
                        //connection.socket.remoteAddress.toJavaAddress().address
                        key = connection,
                        value = Player(
                            playerName, "FF0000"
                        )
                    )
                }
                _gameState.value = GameState.GetPlayerName(playerName)
            }
            // This game state will happen when the user which acts as the server
            // presses a button like "start game"
            is GameState.DisplayCategoryAndWord -> {
                val playersIncludingServer = Server.players.values.toMutableList()
                playersIncludingServer.add(serverPlayer)
                askingPairs = generateAllAskingCombinations(playersIncludingServer)
                // Randomly choose imposter from list of players
                val imposter = Server.players.random().value
                //TODO: Function to choose category and word
                val category = "Animals"
                val word = "Fox"
                // Send category and word to all players except for imposter
                // which only receives category
                for ((clientConnection, player) in Server.players) {
                    if (player == imposter) {
                        clientConnection.output.writeStringUtf8(
                            gson.toJson(
                                mapOf(
                                    "category" to category
                                )
                            )
                        )
                        continue
                    }
                    clientConnection.output.writeStringUtf8(
                        gson.toJson(
                            mapOf(
                                "category" to category,
                                "word" to word
                            )
                        )
                    )
                }
                //TODO: Send first asking pair to all players
                for ((clientConnection, player) in Server.players) {
                    clientConnection.output.writeStringUtf8(
                        gson.toJson(
                            mapOf(
                                "asker" to askingPairs[currentAskingPairIndex].first,
                                "asked" to askingPairs[currentAskingPairIndex].second,
                                "isAsking" to (askingPairs[currentAskingPairIndex].first == player)
                            )
                        )
                    )
                }
                //TODO: Pass real Player objects to AskQuestion and properly check the server is
                // the one asking
                _gameState.value = GameState.AskQuestion(
                    askingPairs[currentAskingPairIndex].first,
                    askingPairs[currentAskingPairIndex].second,
                    (askingPairs[currentAskingPairIndex].first == serverPlayer)
                )
            }

            is GameState.AskQuestion -> {
                //This is sent from the player asking, confirming to end question
                val isDone = gson.fromJson(json, Boolean::class.java)
                currentAskingPairIndex++
                if (currentAskingPairIndex == askingPairs.size) {
                    _gameState.value = GameState.StartVote
                    return
                    //TODO: IDEA! gameState should be a stateflow/flow and you just change it's value
                    // here in this function but what you do when the state changes should be in
                    // the collect function of the flow
                }
                //TODO: Send next question to clients
                for ((clientConnection, player) in Server.players) {
                    clientConnection.output.writeStringUtf8(
                        gson.toJson(
                            mapOf(
                                "asker" to askingPairs[currentAskingPairIndex].first,
                                "asked" to askingPairs[currentAskingPairIndex].second,
                                "isAsking" to (askingPairs[currentAskingPairIndex].first == player)
                            )
                        )
                    )
                }
                _gameState.value = GameState.AskQuestion(
                    askingPairs[currentAskingPairIndex].first,
                    askingPairs[currentAskingPairIndex].second,
                    (askingPairs[currentAskingPairIndex].first == serverPlayer)
                )
            }
            // This game state will happen when the user which acts as the server
            // presses a button like "start vote"
            // (The screen will be like any additional questions?)
            GameState.StartVote -> {
                //TODO: Send messages to all players informing them to start the voting
                // Players list should be sent at start of the game to all players where they
                // get all other players lists not their ips just Player objects
                for ((clientConnection, player) in Server.players) {
                    clientConnection.output.writeStringUtf8(
                        gson.toJson(
                            Server.players.values.toMutableList().remove(player)
                        )
                    )
                }
            }

            is GameState.GetPlayerVote -> {
                //This is sent from the player asking, confirming to end question
                val votedPlayer = gson.fromJson(json, Player::class.java)
                votedPlayers[votedPlayer] = votedPlayers[votedPlayer]!! + 1
                numberOfVoters++
                if (numberOfVoters > Server.players.size) {
                    _gameState.value = GameState.EndVote(votedPlayers.maxBy { it.value }.key)
                    return
                }
            }

            is GameState.EndVote -> {
                //TODO: This event is triggered when all players have voted and server has votes
                // Shouldn't send anything here
            }

            GameState.ShowScoreboard -> {
                //TODO: This should send the votes list for now but should have a score list that
                // keep track of player scores across multiple consecutive rounds
                for ((clientConnection, _) in Server.players) {
                    clientConnection.output.writeStringUtf8(
                        gson.toJson(
                            votedPlayers
                        )
                    )
                }
            }

            GameState.Replay -> {
                //TODO: For now this should send a message to all clients informing them that
                // the game will be continued, for now also this will be determined by the server
                // no voting or whatever
                for ((clientConnection, _) in Server.players) {
                    clientConnection.output.writeStringUtf8(
                        gson.toJson(
                            mapOf("replay" to true)
                        )
                    )
                }
            }
        }
    }

    companion object {
        private const val BASE_SERVICE_NAME = "NsdChat"
        private const val SERVICE_TYPE = "_nsdchat._tcp."
        private const val CODE_LENGTH = 4
    }
}