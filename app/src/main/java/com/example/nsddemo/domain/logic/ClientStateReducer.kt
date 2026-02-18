package com.example.nsddemo.domain.logic

import android.util.Log
import com.example.nsddemo.core.util.Debugging.TAG
import com.example.nsddemo.domain.model.GameData
import com.example.nsddemo.domain.model.RoundData
import com.example.nsddemo.domain.model.ServerMessage

object ClientStateReducer {
    fun reduce(data: GameData, message: ServerMessage): GameData {
        return when (message) {
            // --- LOBBY & SETUP ---
            is ServerMessage.PlayerList -> {
                // Full replacement of player list
                val playerMap = message.players.associateBy { it.id }
                data.copy(players = playerMap)
            }

            is ServerMessage.RegisterHost -> {
                data.copy(hostId = message.hostId)
            }

            is ServerMessage.CategorySelected -> {
                data.copy(category = message.category)
            }

            // --- ROLE DISTRIBUTION ---
            is ServerMessage.RoleAssigned -> {
                // Server tells us our specific role details
                val isImposter = message.word.isEmpty()
                data.copy(
                    category = message.category,
                    word = if (isImposter) null else message.word,
                    imposterId = if (isImposter) data.localPlayerId else null
                )
            }

            is ServerMessage.PlayerReady ->
                data.copy(readyPlayerIds = message.readyPlayerIds.toSet())

            // --- GAMEPLAY (ROUND) ---
            is ServerMessage.Question -> {
                val newRoundData: RoundData.QuestionRoundData
                val newPair = message.askerId to message.askedId
                if (data.roundData is RoundData.Idle) {
                    newRoundData = RoundData.QuestionRoundData(
                        roundPairs = listOf(newPair), currentPairIndex = 0
                    )
                } else {
                    val currentRoundData = data.roundData as RoundData.QuestionRoundData
                    newRoundData = RoundData.QuestionRoundData(
                        roundPairs = currentRoundData.roundPairs + newPair,
                        currentPairIndex = currentRoundData.currentPairIndex + 1
                    )
                }

                data.copy(
                    roundData = newRoundData,
                )
            }

            is ServerMessage.RoundEnd -> data

            is ServerMessage.ReplayRound -> data.copy(
                roundNumber = data.roundNumber + if (message.incrementRoundNumber) 1 else 0,
                roundData = RoundData.Idle
            )
            // --- VOTING & RESULTS ---
            is ServerMessage.StartVote -> data

            is ServerMessage.PlayerVoted ->
                data.copy(votes = data.votes + (message.playerId to message.votedPlayerId))

            is ServerMessage.VotesAfterLeaver -> data.copy(votes = message.votes)

            is ServerMessage.ScoresAfterLeaver -> data.copy(scores = message.scores)

            is ServerMessage.VoteResult -> data.copy(
                votes = message.voteResult,
                imposterId = message.imposterId,
                scores = message.playerScores
            )

            // --- GAME END ---
            is ServerMessage.ContinueToGameChoice -> data

            is ServerMessage.ReplayGame -> GameData(
                localPlayerId = data.localPlayerId,
                hostId = data.hostId,
                gameCode = data.gameCode,
                players = data.players,
                scores = data.scores
            )

            is ServerMessage.EndGame -> GameData()

            // --- SESSION MANAGEMENT ---
            is ServerMessage.PlayerDisconnected -> {
                val disconnectedPlayer =
                    data.players[message.playerId]?.copy(isConnected = false)
                if (disconnectedPlayer != null) {
                    data.copy(players = data.players + (disconnectedPlayer.id to disconnectedPlayer))
                } else {
                    Log.w(
                        TAG,
                        "ClientStateReducer: Couldn't find player who disconnected in player list."
                    )
                    data
                }
            }

            is ServerMessage.PlayerReconnected ->
                data.copy(players = data.players + (message.player.id to message.player))


            is ServerMessage.ReconnectionFullStateSync ->
                message.data.copy(localPlayerId = data.localPlayerId)


            // --- MESSAGES WITHOUT DATA UPDATES (handled elsewhere) ---
            is ServerMessage.GameFull -> data
            is ServerMessage.GameAlreadyStarted -> data
            is ServerMessage.GameResumed -> data.copy(phaseAfterPause = null)
            ServerMessage.LobbyClosed -> data
            ServerMessage.YouWereKicked -> data
        }
    }
}