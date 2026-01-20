package com.example.nsddemo.domain.logic

import com.example.nsddemo.domain.model.NewGameData
import com.example.nsddemo.domain.model.RoundData
import com.example.nsddemo.domain.model.ServerMessage

object ClientStateReducer {
    fun reduce(data: NewGameData, message: ServerMessage): NewGameData {
        return when (message) {
            // --- LOBBY & SETUP ---
            is ServerMessage.PlayerList -> {
                // Full replacement of player list
                val playerMap = message.players.associateBy { it.id }
                data.copy(players = playerMap)
            }

            is ServerMessage.CategorySelected -> {
                data.copy(category = message.category)
            }

            // --- ROLE DISTRIBUTION ---
            is ServerMessage.RoleAssigned -> {
                // Server tells us our specific role details
                // Note: The phase change is handled by GameFlowRegistry, here we just set data
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
                roundNumber = data.roundNumber + 1,
                roundData = RoundData.Idle
            )
            // --- VOTING & RESULTS ---
            is ServerMessage.StartVote -> data

            is ServerMessage.PlayerVoted ->
                data.copy(votes = data.votes + (message.playerId to message.votedPlayerId))


            is ServerMessage.VoteResult -> data.copy(
                votes = message.voteResult,
                imposterId = message.imposterId,
                scores = message.playerScores
            )

            // --- GAME END ---
            is ServerMessage.ContinueToGameChoice -> data

            is ServerMessage.ReplayGame -> NewGameData(
                localPlayerId = data.localPlayerId,
                hostId = data.hostId,
                gameCode = data.gameCode,
                players = data.players,
                scores = data.scores
            )

            is ServerMessage.EndGame -> NewGameData()

            // --- SESSION MANAGEMENT ---
            is ServerMessage.PlayerDisconnected -> {
                val p = data.players[message.playerId]?.copy(isConnected = false)
                if (p != null) {
                    data.copy(players = data.players + (p.id to p))
                } else {
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
            is ServerMessage.GameResumed -> data
        }
    }
}