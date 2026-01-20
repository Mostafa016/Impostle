package com.example.nsddemo.domain.logic

import com.example.nsddemo.domain.model.Active
import com.example.nsddemo.domain.model.ClientMessage
import com.example.nsddemo.domain.model.Envelope
import com.example.nsddemo.domain.model.GamePhase
import com.example.nsddemo.domain.model.GameStateTransition
import com.example.nsddemo.domain.model.NewGameData
import com.example.nsddemo.domain.model.Player
import com.example.nsddemo.domain.model.RoundData
import com.example.nsddemo.domain.model.ServerMessage
import com.example.nsddemo.domain.model.SystemEvent
import com.example.nsddemo.domain.util.GameFlowRegistry
import com.example.nsddemo.domain.util.PlayerCountLimits

class SessionManager {
    fun registerPlayer(
        data: NewGameData, phase: GamePhase, message: ClientMessage.RegisterPlayer
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
        data: NewGameData,
        phase: GamePhase,
        event: SystemEvent
    ): GameStateTransition {
        return when (event) {
            is SystemEvent.PlayerDisconnected -> handleDisconnect(data, phase, event.playerId)
        }
    }

    private fun performReconnection(
        existingPlayer: Player,
        data: NewGameData,
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
            val allConnected = newData.players.values.all { it.isConnected }
            if (allConnected) {
                newPhase = newData.phaseBeforePause!!
                newData = newData.copy(phaseBeforePause = null)
                broadcastMessages.add(ServerMessage.GameResumed)
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
            imposterId = if (message.playerId == newData.imposterId) newData.imposterId else null,
            word = if (message.playerId == newData.imposterId) newData.word else null,
            roundData = syncRoundData,
        )
        return GameStateTransition.Valid(
            newGameData = newData,
            newPhase = newPhase ?: phase,
            envelopes = listOf(
                Envelope.Unicast(
                    recipientId = message.playerId,
                    message = ServerMessage.ReconnectionFullStateSync(
                        data = syncData,
                        phase = newPhase ?: phase
                    )
                )
            ) + broadcastMessages.map {
                Envelope.Broadcast(it)
            }
        )
    }

    private fun performNewJoin(
        data: NewGameData,
        message: ClientMessage.RegisterPlayer,
        phase: GamePhase
    ): GameStateTransition {
        val allowedPhases = GameFlowRegistry.getValidPhasesFor(message)
        if (phase !in allowedPhases) {
            return GameStateTransition.Invalid(
                reason = "Game has already started.",
                envelopes = listOf(
                    Envelope.Unicast(
                        recipientId = message.playerId,
                        message = ServerMessage.GameAlreadyStarted
                    )
                )
            )
        }
        if (data.players.size >= PlayerCountLimits.MAX_PLAYERS) {
            return GameStateTransition.Invalid(
                reason = "Game is already full",
                envelopes = listOf(
                    Envelope.Unicast(
                        recipientId = message.playerId,
                        message = ServerMessage.GameFull
                    )
                )
            )
        }

        val newColor = ColorAllocator.assignColor(data.usedColors)
        val newPlayer =
            Player(name = message.playerName, id = message.playerId, color = newColor.toString())
        val newData = data.copy(players = data.players + (message.playerId to newPlayer))

        return GameStateTransition.Valid(
            newGameData = newData,
            envelopes = listOf(
                Envelope.Broadcast(
                    ServerMessage.PlayerList(newData.players.values.toList())
                )
            )
        )
    }

    private fun handleDisconnect(
        data: NewGameData,
        phase: GamePhase,
        playerId: String
    ): GameStateTransition {
        val player = data.players[playerId]
            ?: return GameStateTransition.Invalid("Can't find disconnect player")

        val ghostPlayer = player.copy(isConnected = false)
        val dataWithGhost = data.copy(players = data.players + (playerId to ghostPlayer))

        return if (phase is Active || phase is GamePhase.Paused) {
            // ===========================
            // CASE: ACTIVE OR ALREADY PAUSED
            // ===========================
            val phaseToSave =
                if (phase is GamePhase.Paused) dataWithGhost.phaseBeforePause else phase
            val pausedData = dataWithGhost.copy(
                phaseBeforePause = phaseToSave
            )

            GameStateTransition.Valid(
                newGameData = pausedData,
                newPhase = GamePhase.Paused,
                envelopes = listOf(
                    Envelope.Broadcast(ServerMessage.PlayerDisconnected(playerId))
                )
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
                newGameData = newData,
                envelopes = listOf(
                    Envelope.Broadcast(ServerMessage.PlayerList(newData.players.values.toList()))
                )
            )
        }
    }
}