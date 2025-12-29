package com.example.nsddemo.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.nsddemo.core.util.Debugging
import com.example.nsddemo.core.util.GameState
import com.example.nsddemo.data.local.network.NSDHelper
import com.example.nsddemo.data.local.network.WifiHelper
import com.example.nsddemo.data.local.network.socket.Server
import com.example.nsddemo.data.local.network.socket.Server.initServerSocket
import com.example.nsddemo.data.repository.GameRepository
import com.example.nsddemo.domain.model.Player
import com.example.nsddemo.domain.repository.ServerNetworkRepository
import com.google.gson.Gson
import io.ktor.network.sockets.Connection
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class ServerViewModel(
    private val gameRepository: GameRepository,
    private val wifiHelper: WifiHelper,
    private val nsdHelper: NSDHelper,
    private val serverNetworkRepository: ServerNetworkRepository,
) : ViewModel() {
    // TODO: The logic for creating and managing the server should be moved to a repository
    //  to support online multiplayer games in the future
    private val gameState = gameRepository.gameState

    override fun onCleared() {
        super.onCleared()
        Log.d(Debugging.TAG, "ServerViewModel cleared")
        closeServer()
    }

    fun createServer() {
        Log.d(Debugging.TAG, "Registering service...")
        val serverIP: String? = wifiHelper.ipAddress
        Log.d(Debugging.TAG, "Device IP: $serverIP")
        initServerSocket(serverIP!!)
        viewModelScope.launch(Dispatchers.IO) {
            Log.d(Debugging.TAG, "Started server")
            Server.run(::handleClientMessages)
        }
        nsdHelper.registerService(Server.port!!, gameRepository.gameData.value.gameCode!!)
    }

    private fun closeServer() {
        nsdHelper.unregisterService()
        Server.clients.clear()
    }

    // region Client messages handling
    private suspend fun handleClientMessages(connection: Connection) {
        // TODO: Need a way to debug easier
        Log.d(Debugging.TAG, "Waiting for client message...")
        val json = connection.input.readUTF8Line()
        Log.d(Debugging.TAG, "==========Message received on state: ${gameState.value}==========")
        Log.d(Debugging.TAG, "Received from client: $json")
        if (json == null) {
            throw NullPointerException("Received null string from client.")
        }
        val gson = Gson()
        updateStateOnMessageReceivedServerSide(json, connection, gson)
    }

    /* Deserializes the JSON message received from the client, maps it to a game state,and updates
    the game state accordingly
    3 responsibilities!
    TODO Solution: split in 3 different functions. Move allowedStates to GameData and update it on each
        screen change thus being able to access it in gameRepository straight away to validate the state
        transitions.
    */
    private fun updateStateOnMessageReceivedServerSide(
        json: String, connection: Connection, gson: Gson
    ) {
        when (val currentGameState = gameState.value) {
            GameState.StartGame, is GameState.GetPlayerInfo -> {
                val playerName = gson.fromJson(json, String::class.java)
                gameRepository.updateGameState(GameState.GetPlayerInfo(playerName, connection))
            }

            is GameState.DisplayCategoryAndWord -> {
                gameRepository.updateGameState(
                    GameState.GetPlayerReadCategoryAndWordConfirmation(1)
                )
            }

            // is GameState.GetPlayerReadCategoryAndWordConfirmation, is GameState.ConfirmCurrentPlayerReadCategoryAndWord
            is GameState.ConfirmReadCategoryAndWord -> {
                gameRepository.updateGameState(
                    GameState.GetPlayerReadCategoryAndWordConfirmation(currentGameState.numberOfConfirmations + 1)
                )
            }

            is GameState.AskQuestion -> {
                // TODO: this and askNextQuestionOrGoToExtraQuestionsChoice in StateManager should be merged
                // This is sent from the player asking, confirming to end question
                val isLastQuestionConfirmation = gson.fromJson(json, Boolean::class.java)
                Log.d(Debugging.TAG, "isLastQuestion = $isLastQuestionConfirmation received.")
                if (isLastQuestionConfirmation) {
                    Log.d(Debugging.TAG, "Choosing to either ask extra questions or start vote...")
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

            GameState.StartVote, is GameState.GetPlayerVote, is GameState.GetCurrentPlayerVote -> {
                val votedPlayer = gson.fromJson(json, Player::class.java)
                gameRepository.updateGameState(
                    GameState.GetPlayerVote(
                        Server.clients[connection]!!, votedPlayer
                    )
                )
            }

            else -> {
                throw IllegalStateException("Received a message while in $currentGameState state.")
            }
        }
    }
    // endregion

    companion object {
        class ServerViewModelFactory(
            private val gameRepository: GameRepository,
            private val wifiHelper: WifiHelper,
            private val nsdHelper: NSDHelper,
            private val serverNetworkRepository: ServerNetworkRepository
        ) : ViewModelProvider.NewInstanceFactory() {
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ServerViewModel(gameRepository, wifiHelper, nsdHelper, serverNetworkRepository) as T
        }
    }
}