package com.example.nsddemo.presentation.navigation

import com.example.nsddemo.domain.model.GamePhase
import com.example.nsddemo.presentation.util.Routes

class GameRouteMapper {
    fun mapToRoute(phase: GamePhase): String? {
        return when (phase) {
            is GamePhase.Lobby -> Routes.GameSessionGraph.route
            is GamePhase.RoleDistribution -> Routes.RoleReveal.route
            is GamePhase.InRound -> Routes.Question.route
            is GamePhase.RoundReplayChoice -> Routes.ReplayRoundChoice.route
            is GamePhase.GameVoting -> Routes.Voting.route
            is GamePhase.GameResults -> Routes.VotingResults.route
            is GamePhase.GameReplayChoice -> Routes.Scoreboard.route
            is GamePhase.Paused -> Routes.Paused.route
            is GamePhase.GameEnd -> Routes.EndGame.route
            // - Idle: means the game ended or hasn't started,
            // we don't force navigation here (handled by EndGameViewModel or MainMenu)
            is GamePhase.Idle -> null
        }
    }
}