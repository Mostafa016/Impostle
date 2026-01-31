package com.example.nsddemo.domain.util

import com.example.nsddemo.domain.model.ClientMessage
import com.example.nsddemo.domain.model.GamePhase
import com.example.nsddemo.domain.model.ServerMessage

object GameFlowRegistry {

    /**
     * Client-Side Logic:
     * Maps an incoming Server Message to a GamePhase transition.
     * Returns `null` if the message is purely for data update and shouldn't trigger navigation.
     */
    fun getTransitionFor(message: ServerMessage): GamePhase? = when (message) {
        // Data updates that occur within the Lobby (UI updates, no navigation)
        // PlayerList can also be used to signal players leaving in Lobby
        is ServerMessage.PlayerList -> null
        is ServerMessage.RegisterHost -> GamePhase.Lobby
        is ServerMessage.CategorySelected -> null

        // Trigger to move from Lobby -> Role Distribution
        is ServerMessage.RoleAssigned -> GamePhase.RoleDistribution

        // Data update within Role Distribution (e.g., "3/4 players ready")
        is ServerMessage.PlayerReady -> null

        // Trigger to move from Role Distribution -> In Round
        // Also acts as the "Next Turn" signal within the round
        is ServerMessage.Question -> GamePhase.InRound

        // Trigger to move from In Round -> Choice Screen
        ServerMessage.RoundEnd -> GamePhase.RoundReplayChoice

        // Trigger to restart the round mechanics.
        ServerMessage.ReplayRound -> GamePhase.InRound

        // Trigger to move from Choice Screen -> Voting
        ServerMessage.StartVote -> GamePhase.GameVoting

        // Data update within Voting (e.g., "Player A has voted")
        is ServerMessage.PlayerVoted -> null

        // Trigger to move from Voting -> Results
        is ServerMessage.VoteResult -> GamePhase.GameResults

        // Trigger to navigate from Results (voting & scores) to the replay choice for the host client
        ServerMessage.ContinueToGameChoice -> GamePhase.GameReplayChoice

        // Trigger to move from Results -> Lobby (keeping scores)
        ServerMessage.ReplayGame -> GamePhase.Lobby

        // Trigger to move to Main Menu / Disconnect
        ServerMessage.EndGame -> GamePhase.Idle

        // Used to tell clients that they cannot join
        ServerMessage.GameAlreadyStarted, ServerMessage.GameFull -> GamePhase.Idle

        // Data updates on current players (Disconnection triggers pause)
        is ServerMessage.PlayerDisconnected -> GamePhase.Paused
        is ServerMessage.PlayerReconnected -> null

        // Full data and phase update for a rejoining player
        is ServerMessage.ReconnectionFullStateSync -> null

        // Trigger unpausing the game (reverting to phaseBeforePause)
        ServerMessage.GameResumed -> null
    }

    /**
     * Server-Side Logic:
     * Defines which Client Actions are legally allowed in which Phase.
     */
    fun getValidPhasesFor(message: ClientMessage): Set<GamePhase> = when (message) {
        // Players can join as soon as the game start, it starts in Lobby
        is ClientMessage.RegisterPlayer -> setOf(GamePhase.Lobby)

        // Host actions restricted to Lobby
        is ClientMessage.RequestSelectCategory -> setOf(GamePhase.Lobby)
        ClientMessage.RequestStartGame -> setOf(GamePhase.Lobby)

        // Players confirm their role/readiness
        ClientMessage.ConfirmRoleReceived -> setOf(GamePhase.RoleDistribution)

        // Game Loop Actions
        ClientMessage.EndTurn -> setOf(GamePhase.InRound)

        // Post-Round Choices
        ClientMessage.RequestReplayRound -> setOf(GamePhase.RoundReplayChoice)
        ClientMessage.RequestStartVote -> setOf(GamePhase.RoundReplayChoice)

        // Voting Action
        is ClientMessage.SubmitVote -> setOf(GamePhase.GameVoting)

        // Post-game Host Actions
        ClientMessage.RequestContinueToGameChoice -> setOf(GamePhase.GameResults)
        ClientMessage.RequestEndGame -> setOf(GamePhase.GameReplayChoice, GamePhase.Paused)
        ClientMessage.RequestReplayGame -> setOf(GamePhase.GameReplayChoice)
    }
}