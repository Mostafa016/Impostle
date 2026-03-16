package com.example.nsddemo.presentation

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Window
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.example.nsddemo.domain.engine.GameSession
import com.example.nsddemo.presentation.navigation.GameNavigation
import com.example.nsddemo.presentation.navigation.GameRouteMapper
import com.example.nsddemo.presentation.screen.choosecategory.ChooseCategoryScreen
import com.example.nsddemo.presentation.screen.creategame.CreateGameLoadingScreen
import com.example.nsddemo.presentation.screen.disconnected.DisconnectedScreen
import com.example.nsddemo.presentation.screen.endgame.EndGameScreen
import com.example.nsddemo.presentation.screen.imposterguess.ImposterGuessScreen
import com.example.nsddemo.presentation.screen.joingame.JoinGameLoadingScreen
import com.example.nsddemo.presentation.screen.joingame.JoinGameScreen
import com.example.nsddemo.presentation.screen.joingame.JoinGameViewModel
import com.example.nsddemo.presentation.screen.lobby.LobbyScreen
import com.example.nsddemo.presentation.screen.mainmenu.MainMenuScreen
import com.example.nsddemo.presentation.screen.pause.PauseScreen
import com.example.nsddemo.presentation.screen.question.QuestionScreen
import com.example.nsddemo.presentation.screen.replayroundchoice.ChooseExtraQuestionsScreen
import com.example.nsddemo.presentation.screen.rolereveal.RoleRevealScreen
import com.example.nsddemo.presentation.screen.score.ScoreScreen
import com.example.nsddemo.presentation.screen.settings.SettingsScreen
import com.example.nsddemo.presentation.screen.settings.SettingsViewModel
import com.example.nsddemo.presentation.screen.voting.VotingScreen
import com.example.nsddemo.presentation.screen.votingresults.VotingResultsScreen
import com.example.nsddemo.presentation.theme.AppTheme
import com.example.nsddemo.presentation.util.NoFeedbackIndication
import com.example.nsddemo.presentation.util.Routes
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var gameSession: GameSession

    @Inject
    lateinit var routeMapper: GameRouteMapper

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        installSplashScreen()

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val useDarkTheme by settingsViewModel.darkThemeSetting.collectAsState()
            val locale by settingsViewModel.languageSetting.collectAsState()
            AppTheme(
                useDarkTheme = useDarkTheme,
                locale = locale,
            ) {
                CompositionLocalProvider(LocalIndication provides NoFeedbackIndication()) {
                    val snackBarHostState = remember { SnackbarHostState() }
                    val scope = rememberCoroutineScope()
                    val navController = rememberNavController()
                    GameNavigation(
                        navController = navController,
                        gameSession = gameSession,
                        routeMapper = routeMapper,
                    )
                    Scaffold(
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                        snackbarHost = { SnackbarHost(snackBarHostState) },
                        contentWindowInsets = ScaffoldDefaults.contentWindowInsets,
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = Routes.MainMenu.route,
                            route = Routes.RootGraph.route,
                        ) {
                            composable(Routes.MainMenu.route) {
                                MainMenuScreen(
                                    navController = navController,
                                )
                            }
                            composable(Routes.Settings.route) {
                                SettingsScreen(
                                    viewModel = settingsViewModel,
                                    navController = navController,
                                )
                            }
                            navigation(
                                startDestination = Routes.JoinGame.route,
                                route = Routes.JoinGameGraph.route,
                            ) {
                                composable(Routes.JoinGame.route) { backStackEntry ->
                                    val parentEntry =
                                        remember(backStackEntry) {
                                            navController.getBackStackEntry(Routes.JoinGameGraph.route)
                                        }
                                    val joinViewModel =
                                        hiltViewModel<JoinGameViewModel>(parentEntry)
                                    JoinGameScreen(
                                        viewModel = joinViewModel,
                                        navController = navController,
                                    )
                                }
                                composable(Routes.JoinGameLoading.route) { backStackEntry ->
                                    val parentEntry =
                                        remember(backStackEntry) {
                                            navController.getBackStackEntry(Routes.JoinGameGraph.route)
                                        }
                                    val joinViewModel =
                                        hiltViewModel<JoinGameViewModel>(parentEntry)
                                    JoinGameLoadingScreen(
                                        viewModel = joinViewModel,
                                        navController = navController,
                                        showSnackBar = { message ->
                                            scope.launch {
                                                snackBarHostState.showSnackbar(
                                                    message = message,
                                                )
                                            }
                                        },
                                    )
                                }
                            }
                            composable(Routes.CreateGameLoading.route) {
                                CreateGameLoadingScreen(navController = navController)
                            }
                            composable(Routes.Paused.route) {
                                PauseScreen()
                            }
                            composable(Routes.Disconnected.route) {
                                DisconnectedScreen(navController = navController)
                            }
                            navigation(
                                startDestination = Routes.Lobby.route,
                                route = Routes.GameSessionGraph.route,
                            ) {
                                composable(Routes.Lobby.route) {
                                    LobbyScreen(navController = navController)
                                }
                                composable(Routes.ChooseCategory.route) {
                                    ChooseCategoryScreen(navController = navController)
                                }
                                composable(Routes.RoleReveal.route) {
                                    RoleRevealScreen()
                                }
                                composable(Routes.Question.route) {
                                    QuestionScreen()
                                }
                                composable(Routes.ReplayRoundChoice.route) {
                                    ChooseExtraQuestionsScreen()
                                }
                                composable(Routes.Voting.route) {
                                    VotingScreen()
                                }
                                composable(Routes.ImposterGuess.route) {
                                    ImposterGuessScreen()
                                }
                                composable(Routes.VotingResults.route) {
                                    VotingResultsScreen()
                                }
                                composable(Routes.Scoreboard.route) {
                                    ScoreScreen(
                                        showSnackBar = { message ->
                                            scope.launch {
                                                snackBarHostState.showSnackbar(
                                                    message = message,
                                                )
                                            }
                                        },
                                    )
                                }
                                composable(Routes.EndGame.route) {
                                    EndGameScreen(
                                        navController = navController,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
