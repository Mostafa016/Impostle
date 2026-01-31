package com.example.nsddemo.domain.engine

import android.util.Log
import com.example.nsddemo.core.util.Debugging.TAG
import com.example.nsddemo.domain.logic.SessionManager
import com.example.nsddemo.domain.model.ClientMessage
import com.example.nsddemo.domain.model.Envelope
import com.example.nsddemo.domain.model.GameAction
import com.example.nsddemo.domain.model.GameMode
import com.example.nsddemo.domain.model.GamePhase
import com.example.nsddemo.domain.model.GameStateTransition
import com.example.nsddemo.domain.model.NewGameData
import com.example.nsddemo.domain.model.PlayerConnectionEvent
import com.example.nsddemo.domain.model.ServerState
import com.example.nsddemo.domain.model.SystemEvent
import com.example.nsddemo.domain.repository.ServerNetworkRepository
import com.example.nsddemo.domain.strategy.GameModeStrategy
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

class GameServer @Inject constructor(
    private val serverNetworkRepository: ServerNetworkRepository,
    private val strategies: Map<GameMode, @JvmSuppressWildcards GameModeStrategy>,
    private val sessionManager: SessionManager
) {
    private var gameModeStrategy: GameModeStrategy = strategies[GameMode.Question]!!
    private val masterGamePhase = MutableStateFlow<GamePhase>(GamePhase.Lobby)
    private val masterGameData =
        MutableStateFlow(NewGameData(roundData = gameModeStrategy.roundData))
    val serverState = serverNetworkRepository.serverState
    private val playerDisconnectionEvents =
        serverNetworkRepository.playerConnectionEvents.mapNotNull { event ->
            when (event) {
                is PlayerConnectionEvent.PlayerConnected -> null

                is PlayerConnectionEvent.PlayerDisconnected -> GameAction.System(
                    SystemEvent.PlayerDisconnected(
                        event.id
                    )
                )
            }
        }

    suspend fun start(gameCode: String, playerId: String) = coroutineScope {
        masterGameData.update { it.copy(gameCode = gameCode, localPlayerId = playerId) }
        val processingJob = launch(start = CoroutineStart.UNDISPATCHED) { processActions() }

        serverNetworkRepository.start(gameCode)

        try {
            withTimeout(TIMEOUT_MS) {
                serverState.first { it !is ServerState.Idle }
            }
        } catch (e: TimeoutCancellationException) {
            processingJob.cancel()
            stop()
        }
    }

    private suspend fun processActions() = coroutineScope {
        merge(serverNetworkRepository.incomingMessages.map { (id, msg) ->
            GameAction.User(id, msg)
        }, playerDisconnectionEvents)
            .collect { action ->
                try {
                    // Log that we are attempting to process an action
                    Log.d(TAG, "GameServer: Processing action: $action")
                    processAction(action)
                    Log.d(TAG, "GameServer: Finished processing action: $action")
                } catch (e: Exception) {
                    // THIS IS THE CRITICAL LOG
                    Log.e(TAG, "GameServer: CRITICAL LOGIC CRASH processing $action", e)
                    throw e // Re-throw to ensure we don't hide the crash, just log it
                }
            }
    }

    private suspend fun processAction(action: GameAction) {
        val transition = when (action) {
            is GameAction.User -> {
                if (action.message is ClientMessage.RegisterPlayer) {
                    Log.d(TAG, "Registering player ${action.playerId}")
                    sessionManager.registerPlayer(
                        data = masterGameData.value,
                        phase = masterGamePhase.value,
                        message = action.message,
                    )
                } else {
                    gameModeStrategy.handleAction(
                        data = masterGameData.value,
                        phase = masterGamePhase.value,
                        message = action.message,
                        playerID = action.playerId
                    )
                }
            }

            is GameAction.System -> {
                sessionManager.handleSystemEvent(
                    data = masterGameData.value, phase = masterGamePhase.value, event = action.event
                )
            }
        }

        handleTransition(transition)
    }

    private suspend fun handleTransition(transition: GameStateTransition) {
        when (transition) {
            is GameStateTransition.Valid -> {
                masterGameData.value = transition.newGameData
                transition.newPhase?.also { masterGamePhase.value = it }
            }

            is GameStateTransition.Invalid -> {
                Log.e(TAG, "Invalid transition: ${transition.reason}")
            }
        }
        sendMessages(transition.envelopes)
    }

    private suspend fun sendMessages(envelopes: List<Envelope>) {
        envelopes.forEach { envelope ->
            when (envelope) {
                is Envelope.Unicast -> serverNetworkRepository.sendToPlayer(
                    envelope.recipientId, envelope.message
                )

                is Envelope.Broadcast -> serverNetworkRepository.sendToAllPlayers(envelope.message)
            }
        }
    }

    suspend fun stop() {
        Log.d(TAG, "GameServer: Stopping server...")
        masterGameData.value = NewGameData()
        masterGamePhase.value = GamePhase.Lobby
        serverNetworkRepository.stop()
    }

    companion object {
        const val TIMEOUT_MS = 10_000L
    }
}