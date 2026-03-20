package com.mostafa.impostle.domain.engine

import android.util.Log
import com.mostafa.impostle.core.util.Debugging.TAG
import com.mostafa.impostle.data.repository.LoopbackClientNetworkRepository
import com.mostafa.impostle.data.repository.RemoteClientNetworkRepository
import com.mostafa.impostle.domain.model.ClientState
import com.mostafa.impostle.domain.model.ServerState
import com.mostafa.impostle.domain.model.SessionState
import com.mostafa.impostle.domain.repository.GameSessionRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
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
class GameSession
    @Inject
    constructor(
        private val clientFactory: GameClient.GameClientFactory,
        private val serverProvider: Provider<GameServer>,
        private val remoteClientRepo: Provider<RemoteClientNetworkRepository>,
        private val loopbackClientRepo: Provider<LoopbackClientNetworkRepository>,
        private val gameSessionRepository: GameSessionRepository,
    ) {
        // Active instances (Null when no game is running)
        var activeClient: GameClient? = null
            private set
        private var gameServer: GameServer? = null

        val gameData = gameSessionRepository.gameData
        val gamePhase = gameSessionRepository.gameState

        private val _sessionState = MutableStateFlow<SessionState>(SessionState.Idle)
        val sessionState: Flow<SessionState> = _sessionState.asStateFlow()

        private var monitorJob: Job? = null

        // --- HOST MODE ---
        suspend fun startHostSession(
            gameCode: String,
            playerId: String,
        ) = coroutineScope {
            reset()

            _sessionState.value = SessionState.Connecting
            gameServer = serverProvider.get()
            activeClient = clientFactory.create(loopbackClientRepo.get())

            launch { gameServer!!.start(gameCode, playerId) }
            launch { activeClient!!.start(gameCode, playerId) }

            try {
                val serverState =
                    withTimeout(GameServer.TIMEOUT_MS) { gameServer?.serverState?.first { it !is ServerState.Idle } }
                val clientState =
                    withTimeout(GameClient.TIMEOUT_MS) {
                        activeClient?.clientState?.first {
                            it is ClientState.Connected ||
                                it is ClientState.Error
                        }
                    }

                _sessionState.value =
                    when {
                        serverState is ServerState.Error -> {
                            stopClientAndServer()
                            SessionState.Error(serverState.message)
                        }

                        clientState is ClientState.Error -> {
                            stopClientAndServer()
                            SessionState.Error(clientState.message)
                        }

                        serverState is ServerState.Running && clientState is ClientState.Connected -> {
                            launch { startRuntimeMonitoring(isHost = true) }
                            SessionState.Running
                        }

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
        suspend fun startJoinSession(
            gameCode: String,
            playerId: String,
        ) = coroutineScope {
            reset()

            _sessionState.value = SessionState.Connecting
            activeClient = clientFactory.create(remoteClientRepo.get())

            launch { activeClient!!.start(gameCode, playerId) }

            try {
                val clientState =
                    withTimeout(GameClient.TIMEOUT_MS) {
                        activeClient?.clientState?.first {
                            it is ClientState.Connected ||
                                it is ClientState.Error
                        }
                    }

                _sessionState.value =
                    when (clientState) {
                        is ClientState.Error -> {
                            activeClient?.stop()
                            SessionState.Error(clientState.message)
                        }

                        is ClientState.Connected -> {
                            launch { startRuntimeMonitoring(isHost = false) }
                            SessionState.Running
                        }

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
            _sessionState.value = SessionState.Idle
            stopClientAndServer()
        }

        private suspend fun stopClientAndServer() {
            gameServer?.stop()
            gameServer = null

            activeClient?.stop()
            activeClient = null
        }

        private suspend fun startRuntimeMonitoring(isHost: Boolean) =
            coroutineScope {
                monitorJob?.cancel()
                monitorJob =
                    launch {
                        if (isHost) {
                            // Host Monitors the SERVER
                            gameServer?.serverState?.collect { state ->
                                if (state is ServerState.Error && _sessionState.value is SessionState.Running) {
                                    _sessionState.value =
                                        SessionState.Error("Server Crashed: ${state.message}")
                                    this.cancel()
                                }
                            }
                        } else {
                            // Client Monitors the NETWORK CONNECTION
                            activeClient?.clientState?.collect { state ->
                                // We only care about Disconnected/Error here because Idle means explicit quit
                                when {
                                    _sessionState.value is SessionState.Running -> {
                                        if (state is ClientState.Disconnected || state is ClientState.Error) {
                                            Log.i(
                                                TAG,
                                                "GameSession: ClientState ($state) while running, setting sessionState as Disconnected",
                                            )
                                            _sessionState.value = SessionState.Disconnected
                                            this.cancel()
                                        } else if (state is ClientState.Idle) {
                                            Log.i(
                                                TAG,
                                                "GameSession: ClientState ($state) while running, setting sessionState as Idle",
                                            )
                                            _sessionState.value = SessionState.Idle
                                            this.cancel()
                                        }
                                    }
                                }
                            }
                        }
                    }
            }
    }
