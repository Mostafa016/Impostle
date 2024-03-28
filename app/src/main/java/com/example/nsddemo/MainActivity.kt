package com.example.nsddemo

import android.annotation.SuppressLint
import android.content.Context
import android.net.nsd.NsdManager
import android.net.wifi.WifiManager
import android.os.Bundle
import android.util.Log
import android.view.Window
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.compose.AppTheme
import com.example.nsddemo.Debugging.TAG
import com.example.nsddemo.ui.GameViewModel
import com.example.nsddemo.ui.category_and_word.CategoryAndWordScreen
import com.example.nsddemo.ui.category_and_word.ChooseCategoryScreen
import com.example.nsddemo.ui.category_and_word.ChooseCategoryViewModel
import com.example.nsddemo.ui.end_game.EndGameScreen
import com.example.nsddemo.ui.game_loading.CreateGameLoadingScreen
import com.example.nsddemo.ui.game_loading.JoinGameLoadingScreen
import com.example.nsddemo.ui.join_game.JoinGameScreen
import com.example.nsddemo.ui.join_game.JoinGameViewModel
import com.example.nsddemo.ui.lobby.LobbyScreen
import com.example.nsddemo.ui.main_menu.MainMenuScreen
import com.example.nsddemo.ui.main_menu.MainMenuViewModel
import com.example.nsddemo.ui.question.ExtraQuestionsScreen
import com.example.nsddemo.ui.question.QuestionScreen
import com.example.nsddemo.ui.score.ScoreScreen
import com.example.nsddemo.ui.settings.SettingsScreen
import com.example.nsddemo.ui.settings.SettingsViewModel
import com.example.nsddemo.ui.voting.VotingScreen
import com.example.nsddemo.ui.voting_results.VotingResultsScreen
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity() {
    @Suppress("UNCHECKED_CAST")
    private val factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GameViewModel(
                nsdManager = getSystemService(Context.NSD_SERVICE) as NsdManager,
                wifiManager = getSystemService(Context.WIFI_SERVICE) as WifiManager,
                sharedPreferences = getPreferences(Context.MODE_PRIVATE),
            ) as T
        }
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val gameViewModel = ViewModelProvider(this, factory)[GameViewModel::class.java]
        val joinGameViewModel = ViewModelProvider(
            this,
            JoinGameViewModel.Companion.JoinGameViewModelFactory(
                gameViewModel,
                getSystemService(NSD_SERVICE) as NsdManager
            )
        )[JoinGameViewModel::class.java]
        val chooseCategoryViewModel = ViewModelProvider(
            this,
            ChooseCategoryViewModel.Companion.ChooseCategoryViewModelFactory(gameViewModel)
        )[ChooseCategoryViewModel::class.java]
        val settingsViewModel = ViewModelProvider(
            this,
            SettingsViewModel.Companion.SettingsViewModelFactory(getPreferences(Context.MODE_PRIVATE))
        )[SettingsViewModel::class.java]
        installSplashScreen()
        setContent {
            AppTheme(
                useDarkTheme = settingsViewModel.darkThemeSetting.value,
                locale = settingsViewModel.languageSetting.value
            ) {
                val snackBarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()
                Scaffold(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .safeContentPadding(),
                    snackbarHost = { SnackbarHost(snackBarHostState) },
                    contentWindowInsets = ScaffoldDefaults.contentWindowInsets
                ) {
                    val navController = rememberNavController()
                    NavHost(
                        navController = navController,
                        startDestination = ScreenRoutes.MainMenu.route
                    ) {
                        composable(ScreenRoutes.MainMenu.route) {
                            MainMenuScreen(gameViewModel,
                                viewModel(
                                    MainMenuViewModel::class.java,
                                    factory = MainMenuViewModel.Companion.MainMenuViewModelFactory(
                                        gameViewModel
                                    )
                                ),
                                onNavigateToCreateGame = {
                                    navController.popBackStackAndNavigateTo(
                                        ScreenRoutes.CreateGame.route
                                    )
                                },
                                onNavigateToJoinGame = {
                                    navController.popBackStackAndNavigateTo(
                                        ScreenRoutes.JoinGame.route
                                    )
                                },
                                onNavigateToSettings = { navController.navigate(ScreenRoutes.Settings.route) }
                            )
                        }
                        composable(ScreenRoutes.Settings.route) {
                            SettingsScreen(settingsViewModel)
                        }
                        composable(ScreenRoutes.CreateGame.route) {
                            CreateGameLoadingScreen(gameViewModel,
                                onGameCreated = {
                                    navController.popBackStackAndNavigateTo(
                                        ScreenRoutes.Lobby.route
                                    )
                                })
                        }
                        composable(ScreenRoutes.JoinGame.route) {
                            JoinGameScreen(
                                gameViewModel, joinGameViewModel,
                                onJoinGamePressed = {
                                    navController.popBackStackAndNavigateTo(ScreenRoutes.JoinGameLoading.route)
                                },
                                onGoBackToMainMenuPressed = {
                                    navController.popBackStackAndNavigateTo(ScreenRoutes.MainMenu.route)
                                },
                            )
                        }
                        composable(ScreenRoutes.JoinGameLoading.route) {
                            //TODO: Add joined players messages to all players so lobby has a meaning
                            // or just keep the loading until host starts game and popBackStackAndNavigateTo to AskQuestion
                            // screen
                            JoinGameLoadingScreen(
                                gameViewModel,
                                joinGameViewModel,
                                onGameJoined = {
                                    navController.popBackStackAndNavigateTo(ScreenRoutes.CategoryAndWord.route)
                                },
                                onGameFound = {
                                    Log.d(TAG, "Game found")
                                    navController.popBackStackAndNavigateTo(ScreenRoutes.Lobby.route)
                                    scope.launch {
                                        snackBarHostState.showSnackbar(getString(R.string.found_game_waiting_for_host_to_start_game))
                                    }
                                },
                                onGameNotFound = {
                                    navController.popBackStackAndNavigateTo(ScreenRoutes.JoinGame.route)
                                    scope.launch {
                                        snackBarHostState.showSnackbar(getString(R.string.game_not_found))
                                    }
                                }
                            )
                        }
                        composable(ScreenRoutes.Lobby.route) {
                            LobbyScreen(
                                gameViewModel = gameViewModel,
                                chooseCategoryViewModel = chooseCategoryViewModel,
                                onChooseCategoryClick = {
                                    navController.popBackStackAndNavigateTo(ScreenRoutes.ChooseCategory.route)
                                },
                                onStartRound = {
                                    navController.popBackStackAndNavigateTo(ScreenRoutes.CategoryAndWord.route)
                                },
                            )
                        }
                        composable(ScreenRoutes.ChooseCategory.route) {
                            ChooseCategoryScreen(
                                vm = chooseCategoryViewModel,
                                onNavigateToLobby = {
                                    navController.popBackStackAndNavigateTo(
                                        ScreenRoutes.Lobby.route
                                    )
                                })
                        }
                        composable(ScreenRoutes.CategoryAndWord.route) {
                            CategoryAndWordScreen(gameViewModel,
                                onNavigateToQuestionScreen = {
                                    navController.popBackStackAndNavigateTo(
                                        ScreenRoutes.Question.route
                                    )
                                })
                        }
                        composable(ScreenRoutes.Question.route) {
                            QuestionScreen(gameViewModel,
                                onNavigateToExtraQuestionsScreen = {
                                    navController.popBackStackAndNavigateTo(
                                        ScreenRoutes.ExtraQuestions.route
                                    )
                                })
                        }
                        composable(ScreenRoutes.ExtraQuestions.route) {
                            ExtraQuestionsScreen(
                                gameViewModel,
                                onNavigateToVotingScreen = {
                                    navController.popBackStackAndNavigateTo(
                                        ScreenRoutes.Voting.route
                                    )
                                },
                                onNavigateToQuestionScreen = {
                                    navController.popBackStackAndNavigateTo(
                                        ScreenRoutes.Question.route
                                    )
                                },
                            )
                        }
                        composable(ScreenRoutes.Voting.route) {
                            VotingScreen(gameViewModel,
                                onNavigateToVotingResultsScreen = {
                                    navController.popBackStackAndNavigateTo(
                                        ScreenRoutes.VotingResults.route
                                    )
                                })
                        }
                        composable(ScreenRoutes.VotingResults.route) {
                            VotingResultsScreen(gameViewModel,
                                onShowScoreClick = {
                                    navController.popBackStackAndNavigateTo(ScreenRoutes.Scoreboard.route)
                                    gameViewModel.onShowScoreClick()
                                },
                                onNavigateToLobbyScreen = {
                                    navController.popBackStackAndNavigateTo(
                                        ScreenRoutes.Lobby.route
                                    )
                                },
                                onNavigateToJoinGameScreen = {
                                    navController.popBackStackAndNavigateTo(
                                        ScreenRoutes.JoinGameLoading.route
                                    )
                                },
                                onNavigateToEndGameScreen = {
                                    navController.popBackStackAndNavigateTo(
                                        ScreenRoutes.EndGame.route
                                    )
                                })
                        }
                        composable(ScreenRoutes.Scoreboard.route) {
                            ScoreScreen(gameViewModel,
                                onPlayAgainPress = gameViewModel.onReplayClick,
                                onNavigateToLobbyScreen = {
                                    navController.popBackStackAndNavigateTo(
                                        ScreenRoutes.Lobby.route
                                    )
                                },
                                onNavigateToJoinGameScreen = {
                                    navController.popBackStackAndNavigateTo(
                                        ScreenRoutes.JoinGameLoading.route
                                    )
                                },
                                onNavigateToEndGameScreen = {
                                    navController.popBackStackAndNavigateTo(
                                        ScreenRoutes.EndGame.route
                                    )
                                })
                        }
                        composable(ScreenRoutes.EndGame.route) {
                            EndGameScreen(
                                gameViewModel = gameViewModel,
                                onNavigateToMainMenu = {
                                    navController.popBackStackAndNavigateTo(
                                        ScreenRoutes.MainMenu.route
                                    )
                                })
                        }
                    }
                }
            }
        }
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    override fun onPause() {
        //tearDownNsdService()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        // registerService(connection.socket.localAddress.toJavaAddress().port, gameCode)
        //discoverServices()
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onDestroy() {
        //tearDownNsdService()
        // connection.socket.close()
        super.onDestroy()
    }


    // NsdHelper's tearDown method
    private fun tearDownNsdService() {
//        nsdManager.apply {
//            unregisterService(registrationListener)
//            stopServiceDiscovery(discoveryListener)
//        }
    }

    private fun NavHostController.popBackStackAndNavigateTo(route: String) {
        this.popBackStack()
        this.navigate(route)
    }
}