package com.example.nsddemo.domain.engine

import com.example.nsddemo.data.repository.LoopbackClientNetworkRepository
import com.example.nsddemo.data.repository.RemoteClientNetworkRepository
import com.example.nsddemo.domain.model.ClientState
import com.example.nsddemo.domain.model.ServerState
import com.example.nsddemo.domain.model.SessionState
import com.example.nsddemo.domain.repository.GameSessionRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class GameSession @Inject constructor(
    private val clientFactory: GameClient.GameClientFactory,
    private val serverProvider: Provider<GameServer>,
    private val remoteClientRepo: RemoteClientNetworkRepository,
    private val loopbackClientRepo: LoopbackClientNetworkRepository,
    private val gameSessionRepository: GameSessionRepository,
) {
    // Active instances (Null when no game is running)
    var activeClient: GameClient? = null
        private set
    private var gameServer: GameServer? = null

    val gameData = gameSessionRepository.gameData
    val gamePhase = gameSessionRepository.gameState

    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Idle)
    val sessionState = _sessionState.asStateFlow()

    // --- HOST MODE ---
    suspend fun startHostSession(gameCode: String, playerId: String) = coroutineScope {
        reset()

        _sessionState.value = SessionState.Connecting
        gameServer = serverProvider.get()
        activeClient = clientFactory.create(loopbackClientRepo)

        launch { gameServer!!.start(gameCode, playerId) }
        launch { activeClient!!.start(gameCode, playerId) }

        try {
            val serverState =
                withTimeout(GameServer.TIMEOUT_MS) { gameServer?.serverState?.first { it !is ServerState.Idle } }
            val clientState =
                withTimeout(GameClient.TIMEOUT_MS) { activeClient?.clientState?.first { it is ClientState.Connected || it is ClientState.Error } }

            _sessionState.value = when {
                serverState is ServerState.Error -> {
                    stopClientAndServer()
                    SessionState.Error(serverState.message)
                }

                clientState is ClientState.Error -> {
                    stopClientAndServer()
                    SessionState.Error(clientState.message)
                }

                serverState is ServerState.Running && clientState is ClientState.Connected -> SessionState.Running
                else -> {
                    stopClientAndServer()
                    SessionState.Error("Couldn't launch session: ServerState: $serverState, ClientState: $clientState")
                }
            }
        } catch (e: TimeoutCancellationException) {
            stopClientAndServer()
            _sessionState.value =
                SessionState.Error("Couldn't start session (timed out): ${e.message}")
        } catch (e: CancellationException) {
            withContext(NonCancellable) {
                reset()
            }
            throw e
        }
    }

    // --- CLIENT MODE ---
    suspend fun startJoinSession(gameCode: String, playerId: String) = coroutineScope {
        reset()

        _sessionState.value = SessionState.Connecting
        activeClient = clientFactory.create(remoteClientRepo)

        launch { activeClient!!.start(gameCode, playerId) }

        try {
            val clientState =
                withTimeout(GameClient.TIMEOUT_MS) { activeClient?.clientState?.first { it is ClientState.Connected || it is ClientState.Error } }

            _sessionState.value = when (clientState) {
                is ClientState.Error -> {
                    activeClient?.stop()
                    SessionState.Error(clientState.message)
                }

                is ClientState.Connected -> SessionState.Running
                else -> {
                    activeClient?.stop()
                    SessionState.Error("Couldn't launch session: ClientState: $clientState")
                }
            }
        } catch (e: TimeoutCancellationException) {
            activeClient?.stop()
            _sessionState.value =
                SessionState.Error("Couldn't start session (timed out): ${e.message}")
        } catch (e: CancellationException) {
            withContext(NonCancellable) {
                reset()
            }
            throw e
        }
    }

    suspend fun reset() {
        stopClientAndServer()
        _sessionState.value = SessionState.Idle
    }

    private suspend fun stopClientAndServer() {
        gameServer?.stop()
        gameServer = null

        activeClient?.stop()
        activeClient = null
    }
}