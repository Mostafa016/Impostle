package com.example.nsddemo.domain.engine

import android.util.Log
import com.example.nsddemo.core.util.Debugging.TAG
import com.example.nsddemo.domain.logic.ClientStateReducer
import com.example.nsddemo.domain.model.ClientEvent
import com.example.nsddemo.domain.model.ClientMessage
import com.example.nsddemo.domain.model.ClientState
import com.example.nsddemo.domain.model.GameCategory
import com.example.nsddemo.domain.model.GamePhase
import com.example.nsddemo.domain.model.ServerMessage
import com.example.nsddemo.domain.repository.ClientNetworkRepository
import com.example.nsddemo.domain.repository.GameSessionRepository
import com.example.nsddemo.domain.util.GameFlowRegistry
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class GameClient @AssistedInject constructor(
    private val gameSessionRepository: GameSessionRepository,
    @Assisted private val clientNetworkRepository: ClientNetworkRepository
) {
    val gameData = gameSessionRepository.gameData
    val gamePhase = gameSessionRepository.gameState

    val clientState = clientNetworkRepository.clientState

    private val _clientEvent = MutableSharedFlow<ClientEvent>()
    val clientEvent = _clientEvent.asSharedFlow()

    suspend fun start(gameCode: String, playerId: String) = coroutineScope {
        gameSessionRepository.updateGameData {
            it.copy(
                gameCode = gameCode,
                localPlayerId = playerId
            )
        }
        val listeningJob = launch(start = CoroutineStart.UNDISPATCHED) { startListening() }

        launch { clientNetworkRepository.connect(gameCode) }

        try {
            withTimeout(TIMEOUT_MS) {
                clientState.first { it is ClientState.Connected }
            }
        } catch (e: TimeoutCancellationException) {
            listeningJob.cancel()
            stop()
        }
    }

    private suspend fun startListening() {
        Log.d(TAG, "GameClient: startListening STARTED")
        clientNetworkRepository.incomingMessages.collect { (_, message) ->
            Log.d(TAG, "GameClient: Received message $message")
            handleServerMessage(message)
        }
        Log.d(TAG, "GameClient: startListening ENDED")
    }

    private suspend fun handleServerMessage(message: ServerMessage) {
        // A. Update Data
        gameSessionRepository.updateGameData { currentData ->
            val gameData = ClientStateReducer.reduce(currentData, message)
            Log.i(TAG, "GameClient: gameData after $message: $gameData")
            gameData
        }

        // B. Update Phase
        when (message) {
            is ServerMessage.YouWereKicked -> {
                _clientEvent.emit(ClientEvent.KickedFromGame)
                stop() // Trigger intentional disconnect (State -> Idle)
                return
            }

            is ServerMessage.LobbyClosed -> {
                _clientEvent.emit(ClientEvent.LobbyClosed)
                stop() // Trigger intentional disconnect (State -> Idle)
                return
            }

            is ServerMessage.GameResumed -> {
                gameSessionRepository.updateGamePhase(message.phaseAfterPause)
            }

            else -> {
                if (gamePhase.value != GamePhase.Paused || message is ServerMessage.EndGame) {
                    val nextPhase = GameFlowRegistry.getTransitionFor(message)
                    if (nextPhase != null && nextPhase != gamePhase.value) {
                        gameSessionRepository.updateGamePhase(nextPhase)
                    }
                }
            }
        }
        Log.i(TAG, "GameClient: gamePhase after $message: ${gamePhase.value}")

        // C. Emit Domain Events
        when (message) {
            is ServerMessage.GameFull -> _clientEvent.emit(ClientEvent.LobbyFull)
            is ServerMessage.GameAlreadyStarted -> _clientEvent.emit(ClientEvent.GameAlreadyStarted)
            is ServerMessage.PlayerDisconnected -> _clientEvent.emit(ClientEvent.PlayerLeft(message.playerId))
            is ServerMessage.PlayerReconnected -> _clientEvent.emit(
                ClientEvent.PlayerRejoined(
                    message.player.id
                )
            )

            is ServerMessage.GameResumed -> {
                _clientEvent.emit(ClientEvent.GameResumed)
            }

            else -> {}
        }
    }

    //region --- Actions ---
    suspend fun registerPlayer(name: String, playerId: String) {
        clientNetworkRepository.sendToServer(ClientMessage.RegisterPlayer(name, playerId))
    }

    suspend fun kickPlayer(playerId: String) {
        clientNetworkRepository.sendToServer(ClientMessage.RequestKickPlayer(playerId))
    }

    suspend fun selectCategory(category: GameCategory) {
        clientNetworkRepository.sendToServer(ClientMessage.RequestSelectCategory(category))
    }

    suspend fun startGame() {
        clientNetworkRepository.sendToServer(ClientMessage.RequestStartGame)
    }

    suspend fun confirmRole() {
        clientNetworkRepository.sendToServer(ClientMessage.ConfirmRoleReceived)
    }

    suspend fun endTurn() {
        clientNetworkRepository.sendToServer(ClientMessage.EndTurn)
    }

    suspend fun startVote() {
        clientNetworkRepository.sendToServer(ClientMessage.RequestStartVote)
    }

    suspend fun replayRound() {
        clientNetworkRepository.sendToServer(ClientMessage.RequestReplayRound)
    }

    suspend fun submitVote(targetPlayerId: String) {
        clientNetworkRepository.sendToServer(ClientMessage.SubmitVote(targetPlayerId))
    }

    suspend fun continueToGameChoice() {
        clientNetworkRepository.sendToServer(ClientMessage.RequestContinueToGameChoice)
    }

    suspend fun replayGame() {
        clientNetworkRepository.sendToServer(ClientMessage.RequestReplayGame)
    }

    suspend fun endGame() {
        clientNetworkRepository.sendToServer(ClientMessage.RequestEndGame)
    }
    //endregion

    suspend fun stop() {
        gameSessionRepository.reset()
        clientNetworkRepository.disconnect()
    }

    companion object {
        const val TIMEOUT_MS = 15_000L
    }

    @AssistedFactory
    interface GameClientFactory {
        fun create(clientNetworkRepository: ClientNetworkRepository): GameClient
    }
}
