package com.example.nsddemo.domain.logic

import android.util.Log
import com.example.nsddemo.core.util.Debugging.TAG
import com.example.nsddemo.domain.model.Active
import com.example.nsddemo.domain.model.ClientMessage
import com.example.nsddemo.domain.model.Envelope
import com.example.nsddemo.domain.model.GameData
import com.example.nsddemo.domain.model.GamePhase
import com.example.nsddemo.domain.model.GameStateTransition
import com.example.nsddemo.domain.model.Player
import com.example.nsddemo.domain.model.RoundData
import com.example.nsddemo.domain.model.ServerMessage
import com.example.nsddemo.domain.model.SystemEvent
import com.example.nsddemo.domain.strategy.GameModeStrategy
import com.example.nsddemo.domain.util.GameFlowRegistry
import com.example.nsddemo.domain.util.PlayerCountLimits
import javax.inject.Inject

class SessionManager @Inject constructor() {
    fun registerPlayer(
        data: GameData, phase: GamePhase, message: ClientMessage.RegisterPlayer
    ): GameStateTransition {
        val existingPlayer = data.players[message.playerId]
        return if (existingPlayer != null) {
            if (existingPlayer.isConnected) {
                return GameStateTransition.Invalid("Can't register: player already connected")
            }
            performReconnection(existingPlayer, data, message, phase)
        } else {
            performNewJoin(data, message, phase)
        }
    }

    fun handleSystemEvent(
        data: GameData, phase: GamePhase, event: SystemEvent
    ): GameStateTransition {
        return when (event) {
            is SystemEvent.PlayerDisconnected -> handleDisconnect(data, phase, event.playerId)
        }
    }

    private fun performReconnection(
        existingPlayer: Player,
        data: GameData,
        message: ClientMessage.RegisterPlayer,
        phase: GamePhase
    ): GameStateTransition {
        // 1. Mark this specific player online
        val updatedPlayer = existingPlayer.copy(isConnected = true, name = message.playerName)
        var newData = data.copy(players = data.players + (message.playerId to updatedPlayer))

        var newPhase: GamePhase? = null
        val broadcastMessages: MutableList<ServerMessage> =
            mutableListOf(ServerMessage.PlayerReconnected(updatedPlayer))
        // 2. AUTO-RESUME CHECK
        if (phase is GamePhase.Paused) {
            if (newData.isEveryoneConnected) {
                newPhase = newData.phaseAfterPause!!
                newData = newData.copy(phaseBeforePause = null, phaseAfterPause = null)
                broadcastMessages.add(ServerMessage.GameResumed(newPhase))
            }
        }

        val syncRoundData = when (val roundData = newData.roundData) {
            is RoundData.Idle -> {
                RoundData.Idle
            }

            is RoundData.QuestionRoundData -> {
                RoundData.QuestionRoundData(
                    roundPairs = roundData.roundPairs.subList(0, roundData.currentPairIndex + 1),
                    currentPairIndex = roundData.currentPairIndex
                )
            }
        }
        val syncData = newData.copy(
            localPlayerId = "",
            imposterId = if (message.playerId == newData.imposterId || (newPhase
                    ?: phase).let { it is GamePhase.GameResults || it is GamePhase.GameReplayChoice || it is GamePhase.GameEnd }
            ) {
                newData.imposterId
            } else {
                null
            },
            word = if (message.playerId == newData.imposterId) null else newData.word,
            roundData = syncRoundData,
        )
        return GameStateTransition.Valid(
            newGameData = newData, newPhase = newPhase ?: phase, envelopes = listOf(
                Envelope.Unicast(
                    recipientId = message.playerId,
                    message = ServerMessage.ReconnectionFullStateSync(
                        data = syncData, phase = newPhase ?: phase
                    )
                )
            ) + broadcastMessages.map {
                Envelope.Broadcast(it)
            })
    }

    private fun performNewJoin(
        data: GameData, message: ClientMessage.RegisterPlayer, phase: GamePhase
    ): GameStateTransition {
        val allowedPhases = GameFlowRegistry.getValidPhasesFor(message)
        if (phase !in allowedPhases) {
            return GameStateTransition.Invalid(
                reason = "Game has already started.", envelopes = listOf(
                    Envelope.Unicast(
                        recipientId = message.playerId, message = ServerMessage.GameAlreadyStarted
                    )
                )
            )
        }
        if (data.players.size >= PlayerCountLimits.MAX_PLAYERS) {
            return GameStateTransition.Invalid(
                reason = "Game is already full", envelopes = listOf(
                    Envelope.Unicast(
                        recipientId = message.playerId, message = ServerMessage.GameFull
                    )
                )
            )
        }

        val newColor = ColorAllocator.assignColor(data.usedColors)
        val newPlayer =
            Player(name = message.playerName, id = message.playerId, color = newColor.toString())
        val newData = data.copy(
            players = data.players + (message.playerId to newPlayer),
            hostId = if (message.playerId == data.localPlayerId) data.localPlayerId else data.hostId
        )

        Log.d(TAG, "SessionManager: Sending the player the messages")
        Log.d(TAG, "SessionManager: newData: $newData")
        val envelopes = listOfNotNull(
            Envelope.Unicast(message.playerId, ServerMessage.RegisterHost(newData.hostId)),
            newData.category?.let {
                Envelope.Unicast(
                    message.playerId,
                    ServerMessage.CategorySelected(it)
                )
            },
            Envelope.Broadcast(ServerMessage.PlayerList(newData.players.values.toList()))
        )
        return GameStateTransition.Valid(
            newGameData = newData,
            envelopes = envelopes
        )
    }

    private fun handleDisconnect(
        data: GameData, phase: GamePhase, playerId: String
    ): GameStateTransition {
        val player = data.players[playerId]
            ?: return GameStateTransition.Invalid("Can't find disconnected player in ${data.players}")

        val ghostPlayer = player.copy(isConnected = false)
        val dataWithGhost = data.copy(players = data.players + (playerId to ghostPlayer))

        return if (phase is Active || phase is GamePhase.Paused) {
            // ===========================
            // CASE: ACTIVE OR ALREADY PAUSED
            // ===========================
            val phaseBeforePause =
                if (phase is GamePhase.Paused) dataWithGhost.phaseBeforePause!! else phase
            val phaseAfterPause =
                if (phase is GamePhase.Paused) dataWithGhost.phaseAfterPause!! else phase
            val pausedData = dataWithGhost.copy(
                phaseBeforePause = phaseBeforePause,
                phaseAfterPause = phaseAfterPause
            )

            GameStateTransition.Valid(
                newGameData = pausedData,
                newPhase = GamePhase.Paused,
                envelopes = listOf(
                    Envelope.Broadcast(ServerMessage.PlayerDisconnected(playerId))
                ),
            )
        } else {
            // ===========================
            // CASE: LOBBY -> DELETE
            // ===========================
            val newData = data.copy(
                players = data.players - playerId,
                scores = data.scores - playerId,
                votes = data.votes - playerId,
                readyPlayerIds = data.readyPlayerIds - playerId
            )

            GameStateTransition.Valid(
                newGameData = newData, envelopes = listOf(
                    Envelope.Broadcast(ServerMessage.PlayerList(newData.players.values.toList()))
                )
            )
        }
    }

    fun kickPlayer(
        data: GameData,
        phase: GamePhase,
        playerIdToKick: String,
        strategy: GameModeStrategy
    ): GameStateTransition {
        // 1. Core Cleanup: Remove player from the map
        val remainingPlayers = data.players.filterKeys { it != playerIdToKick }

        // 2. Minimum Player Check
        if (phase !is GamePhase.Lobby && remainingPlayers.size < PlayerCountLimits.MIN_PLAYERS) {
            return GameStateTransition.Valid(
                newGameData = GameData(), // Reset entirely
                newPhase = GamePhase.GameEnd,
                envelopes = listOf(Envelope.Broadcast(ServerMessage.EndGame))
            )
        }

        // 3. Imposter Check (Win Condition)
        // If the Imposter is kicked, Civilians win immediately.
        val cleanScores = data.scores
            .filterKeys { it != playerIdToKick }
        if (playerIdToKick == data.imposterId && phase !is GamePhase.Lobby && phase !is GamePhase.GameReplayChoice) {
            val cleanedData = data.copy(
                players = remainingPlayers,
                scores = cleanScores,
                phaseBeforePause = null,
                phaseAfterPause = null
            )
            return GameStateTransition.Valid(
                newGameData = cleanedData,
                newPhase = GamePhase.GameResults,
                envelopes = listOf(
                    Envelope.Broadcast(ServerMessage.PlayerList(data.players.values.toList())),
                    Envelope.Broadcast(
                        ServerMessage.VoteResult(
                            voteResult = emptyMap(), // Empty votes imply forfeit
                            imposterId = playerIdToKick,
                            playerScores = cleanScores // Keep existing scores
                        )
                    ),
                    Envelope.Broadcast(ServerMessage.GameResumed(GamePhase.GameResults)),
                )
            )
        }

        // 4. Delegate to Strategy:
        // Important: If we are Paused, we want to calculate the logic based on the
        // phase we were in BEFORE the pause (to know if we need to restart round etc)
        val cleanedData = data.copy(players = remainingPlayers, scores = cleanScores)
        val logicPhase =
            if (phase is GamePhase.Paused) cleanedData.phaseBeforePause!! else phase
        Log.d(TAG, "SessionManager: Kicking player in logic phase: $logicPhase")

        val transition = strategy.onPlayerRemoved(cleanedData, logicPhase, playerIdToKick)

        // 5. Decide to keep pausing or resume
        // Clear Pause State:
        return when (transition) {
            is GameStateTransition.Valid -> {
                transition.let {
                    if (it.newGameData.isEveryoneConnected) {
                        Log.d(TAG, "SessionManager: Resuming game")
                        it.copy(
                            newGameData = it.newGameData.copy(
                                phaseBeforePause = null,
                                phaseAfterPause = null
                            ),
                            newPhase = transition.newPhase ?: logicPhase,
                            envelopes = it.envelopes + Envelope.Broadcast(
                                ServerMessage.GameResumed(
                                    transition.newPhase ?: logicPhase
                                )
                            )
                        )
                    } else {
                        Log.d(TAG, "SessionManager: Keeping game paused")
                        it.copy(
                            newGameData = it.newGameData.copy(phaseAfterPause = it.newPhase),
                            newPhase = GamePhase.Paused
                        )
                    }
                }
            }

            is GameStateTransition.Invalid -> transition
        }

    }
}