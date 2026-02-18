package com.example.nsddemo.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.nsddemo.domain.engine.GameSession
import com.example.nsddemo.domain.model.SessionState
import com.example.nsddemo.presentation.util.Routes

@Composable
fun GameNavigation(
    navController: NavController,
    gameSession: GameSession,
    routeMapper: GameRouteMapper
) {
    val sessionState by gameSession.sessionState.collectAsStateWithLifecycle(SessionState.Idle)
    val gamePhase by gameSession.gamePhase.collectAsStateWithLifecycle()

    LaunchedEffect(sessionState, gamePhase) {
        val targetRoute = when (sessionState) {
            is SessionState.Running -> routeMapper.mapToRoute(gamePhase)
            is SessionState.Disconnected, is SessionState.Error -> Routes.Disconnected.route
            else -> null
        }

        val currentRoute = navController.currentDestination?.route
        if (targetRoute == null || currentRoute == targetRoute) {
            return@LaunchedEffect
        }

        navController.navigate(targetRoute) {
            popUpTo(Routes.RootGraph.route) { inclusive = false }
            launchSingleTop = true
        }
    }

    LaunchedEffect(sessionState) {
        if (sessionState is SessionState.Idle) {
            // If we were in a game and suddenly went Idle (and not because we manually navigated back),
            // it means we were Kicked or Lobby Closed.
            // Check if current destination is a game screen.
            val route = navController.currentDestination?.route
            if (route != Routes.MainMenu.route && route != Routes.JoinGame.route && route != Routes.Disconnected.route) {
                navController.navigate(Routes.MainMenu.route) {
                    popUpTo(Routes.RootGraph.route) { inclusive = true }
                }
            }
        }
    }
}