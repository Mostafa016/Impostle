package com.example.nsddemo.presentation

import android.annotation.SuppressLint
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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import com.example.nsddemo.core.util.Debugging.TAG
import com.example.nsddemo.data.local.network.NSDHelper
import com.example.nsddemo.data.local.network.WifiHelper
import com.example.nsddemo.data.repository.GameRepository
import com.example.nsddemo.data.repository.KtorClientNetworkRepository
import com.example.nsddemo.data.repository.KtorServerNetworkRepository
import com.example.nsddemo.domain.use_case.ServerGameStateManager
import com.example.nsddemo.presentation.screen.category_and_word.CategoryAndWordGameStateHandler
import com.example.nsddemo.presentation.screen.category_and_word.CategoryAndWordScreen
import com.example.nsddemo.presentation.screen.category_and_word.CategoryAndWordViewModel
import com.example.nsddemo.presentation.screen.choose_category.ChooseCategoryGameStateHandler
import com.example.nsddemo.presentation.screen.choose_category.ChooseCategoryScreen
import com.example.nsddemo.presentation.screen.choose_category.ChooseCategoryViewModel
import com.example.nsddemo.presentation.screen.choose_extra_questions.ChooseExtraQuestionsGameStateHandler
import com.example.nsddemo.presentation.screen.choose_extra_questions.ChooseExtraQuestionsScreen
import com.example.nsddemo.presentation.screen.choose_extra_questions.ChooseExtraQuestionsViewModel
import com.example.nsddemo.presentation.screen.create_game.CreateGameLoadingScreen
import com.example.nsddemo.presentation.screen.create_game.CreateGameViewModel
import com.example.nsddemo.presentation.screen.end_game.EndGameScreen
import com.example.nsddemo.presentation.screen.end_game.EndGameViewModel
import com.example.nsddemo.presentation.screen.join_game.JoinGameLoadingScreen
import com.example.nsddemo.presentation.screen.join_game.JoinGameScreen
import com.example.nsddemo.presentation.screen.join_game.JoinGameViewModel
import com.example.nsddemo.presentation.screen.lobby.LobbyGameStateHandler
import com.example.nsddemo.presentation.screen.lobby.LobbyScreen
import com.example.nsddemo.presentation.screen.lobby.LobbyViewModel
import com.example.nsddemo.presentation.screen.main_menu.MainMenuGameStateHandler
import com.example.nsddemo.presentation.screen.main_menu.MainMenuScreen
import com.example.nsddemo.presentation.screen.main_menu.MainMenuViewModel
import com.example.nsddemo.presentation.screen.question.QuestionGameStateHandler
import com.example.nsddemo.presentation.screen.question.QuestionScreen
import com.example.nsddemo.presentation.screen.question.QuestionViewModel
import com.example.nsddemo.presentation.screen.score.ScoreGameStateHandler
import com.example.nsddemo.presentation.screen.score.ScoreScreen
import com.example.nsddemo.presentation.screen.score.ScoreViewModel
import com.example.nsddemo.presentation.screen.settings.SettingsScreen
import com.example.nsddemo.presentation.screen.settings.SettingsViewModel
import com.example.nsddemo.presentation.screen.voting.VotingGameStateHandler
import com.example.nsddemo.presentation.screen.voting.VotingScreen
import com.example.nsddemo.presentation.screen.voting.VotingViewModel
import com.example.nsddemo.presentation.screen.voting_results.VotingResultsGameStateHandler
import com.example.nsddemo.presentation.screen.voting_results.VotingResultsScreen
import com.example.nsddemo.presentation.screen.voting_results.VotingResultsViewModel
import com.example.nsddemo.presentation.theme.AppTheme
import com.example.nsddemo.presentation.util.NavigationUtil.sharedViewModel
import com.example.nsddemo.presentation.util.Routes
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity() {
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val sharedPreferences = getPreferences(MODE_PRIVATE)
        val gson = Gson()
        val gameRepository = GameRepository(sharedPreferences)
        // TODO: To be moved to its own function for debugging purposes
        lifecycleScope.launch(Dispatchers.IO) {
            gameRepository.gameState.collect {
                Log.d(TAG, "**********GameState changed to $it**********")
            }
        }
        val serverGameStateManager = ServerGameStateManager(gameRepository, gson)
        val nsdHelper = NSDHelper(this)
        val wifiHelper = WifiHelper(this)
        val ktorServerNetworkRepository =
            KtorServerNetworkRepository(nsdHelper = nsdHelper, wifiHelper = wifiHelper)
        val ktorClientNetworkRepository =
            KtorClientNetworkRepository(nsdHelper = nsdHelper, wifiHelper = wifiHelper)
        val settingsViewModel = ViewModelProvider(
            this, SettingsViewModel.Companion.SettingsViewModelFactory(sharedPreferences)
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
//                    val isHostState by remember { mutableStateOf(gameRepository.gameData) }.value.collectAsState()
//                    var gameSessionRouteStartDestination: String by remember { mutableStateOf(Routes.JoinGameSession.route) }
//                    LaunchedEffect(isHostState) {
//                        Log.d(TAG, "isHostState: $isHostState")
//                        val isHost = isHostState.isHost ?: return@LaunchedEffect
//                        gameSessionRouteStartDestination = if (isHost) {
//                            Routes.CreateGame.route
//                        } else {
//                            Routes.JoinGame.route
//                        }
//                        Log.d(TAG, "gameSessionRouteStartDestination: $gameSessionRouteStartDestination")
//                    }
                    // TODO: Scoping ViewModels
                    //  Sol#1: Fuck it: Remove all nested NavGraphs and clear resources manually for
                    //  client and server ViewModels for now
                    //  Idea#1: Navigate WITHOUT popping nav backstack entry
                    //  Idea#2: Navigate THEN pop because if you pop first then
                    //  you will lose the parent nav graph (speculation)
                    //  NOTE#1: Currently JoinGameViewModel lives correctly investigate to replicate
                    //  NOTE#2: Revise routes in navigation before doing Note#1
                    val navController = rememberNavController()
                    NavHost(
                        navController = navController, startDestination = Routes.MainMenu.route
                    ) {
                        composable(Routes.MainMenu.route) {
                            MainMenuScreen(
                                navController = navController, viewModel = viewModel(
                                    MainMenuViewModel::class.java,
                                    factory = MainMenuViewModel.Companion.MainMenuViewModelFactory(
                                        gameRepository,
                                        MainMenuGameStateHandler(
                                            gameRepository, serverGameStateManager
                                        )
                                    )
                                )
                            )
                        }
                        composable(Routes.Settings.route) {
                            SettingsScreen(settingsViewModel)
                        }
                        navigation(
                            startDestination = Routes.JoinGameSession.route,
                            route = Routes.GameSession.route
                        ) {
                            // This is used to scope ServerViewModel and ClientViewModel correctly
                            // TODO: This will probably break some navigation methods it should be
                            //  tested but it should work fine after
                            navigation(
                                startDestination = Routes.JoinGame.route,
                                route = Routes.JoinGameSession.route
                            ) {
                                composable(Routes.JoinGame.route) {
                                    val gameSessionEntry = remember(it) {
                                        navController.getBackStackEntry(Routes.GameSession.route)
                                    }
                                    navController.previousBackStackEntry
                                    val clientViewModel = viewModel(
                                        ClientViewModel::class.java,
                                        viewModelStoreOwner = gameSessionEntry,
                                        factory = ClientViewModel.Companion.ClientViewModelFactory(
                                            gameRepository, nsdHelper, ktorClientNetworkRepository
                                        )
                                    )
                                    JoinGameScreen(
                                        viewModel = it.sharedViewModel<JoinGameViewModel>(
                                            navController = navController,
                                            factory = JoinGameViewModel.Companion.JoinGameViewModelFactory(
                                                clientViewModel, gameRepository, nsdHelper
                                            )
                                        ), navController = navController
                                    )
                                }
                                composable(Routes.JoinGameLoading.route) {
                                    val gameSessionEntry = remember(it) {
                                        navController.getBackStackEntry(Routes.GameSession.route)
                                    }
                                    val clientViewModel = viewModel(
                                        ClientViewModel::class.java,
                                        viewModelStoreOwner = gameSessionEntry,
                                        factory = ClientViewModel.Companion.ClientViewModelFactory(
                                            gameRepository, nsdHelper,
                                            clientNetworkRepository = ktorClientNetworkRepository
                                        )
                                    )
                                    JoinGameLoadingScreen(
                                        viewModel = it.sharedViewModel<JoinGameViewModel>(
                                            navController = navController,
                                            factory = JoinGameViewModel.Companion.JoinGameViewModelFactory(
                                                clientViewModel, gameRepository, nsdHelper
                                            )
                                        ),
                                        navController = navController,
                                        showSnackBar = { message ->
                                            scope.launch {
                                                snackBarHostState.showSnackbar(
                                                    message = message,
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                            composable(Routes.CreateGame.route) {
                                val gameSessionNavGraphRoute = Routes.GameSession.route
                                val gameSessionEntry = remember(it) {
                                    navController.getBackStackEntry(gameSessionNavGraphRoute)
                                }
                                val serverViewModel: ServerViewModel = viewModel(
                                    viewModelStoreOwner = gameSessionEntry,
                                    factory = ServerViewModel.Companion.ServerViewModelFactory(
                                        gameRepository,
                                        wifiHelper,
                                        nsdHelper,
                                        ktorServerNetworkRepository
                                    )
                                )
                                CreateGameLoadingScreen(
                                    viewModel = viewModel(
                                        CreateGameViewModel::class.java,
                                        factory = CreateGameViewModel.Companion.CreateGameViewModelFactory(
                                            serverViewModel = serverViewModel, nsdHelper = nsdHelper
                                        )
                                    ), navController = navController
                                )
                            }
                            composable(Routes.Lobby.route) {
                                LobbyScreen(
                                    navController = navController, viewModel = viewModel(
                                        LobbyViewModel::class.java,
                                        factory = LobbyViewModel.Companion.LobbyViewModelFactory(
                                            gameRepository = gameRepository,
                                            gameStateHandler = LobbyGameStateHandler(
                                                gameRepository = gameRepository,
                                                serverGameStateManager = serverGameStateManager,
                                            )
                                        )
                                    )
                                )
                            }
                            composable(Routes.ChooseCategory.route) {
                                ChooseCategoryScreen(
                                    viewModel = viewModel(
                                        ChooseCategoryViewModel::class.java,
                                        factory = ChooseCategoryViewModel.Companion.ChooseCategoryViewModelFactory(
                                            gameRepository, ChooseCategoryGameStateHandler(
                                                gameRepository, serverGameStateManager
                                            )
                                        )
                                    ), navController = navController
                                )
                            }
                            composable(Routes.CategoryAndWord.route) {
                                CategoryAndWordScreen(
                                    viewModel = viewModel(
                                        CategoryAndWordViewModel::class.java,
                                        factory = CategoryAndWordViewModel.Companion.CategoryAndWordViewModelFactory(
                                            gameRepository, CategoryAndWordGameStateHandler(
                                                gameRepository, serverGameStateManager
                                            )
                                        )
                                    ), navController = navController
                                )
                            }
                            composable(Routes.Question.route) {
                                QuestionScreen(
                                    viewModel = viewModel(
                                        QuestionViewModel::class.java,
                                        factory = QuestionViewModel.Companion.QuestionViewModelFactory(
                                            gameRepository = gameRepository,
                                            gameStateHandler = QuestionGameStateHandler(
                                                gameRepository = gameRepository,
                                                serverGameStateManager = serverGameStateManager
                                            )
                                        )
                                    ),
                                    navController = navController,
                                )
                            }
                            composable(Routes.ChooseExtraQuestions.route) {
                                ChooseExtraQuestionsScreen(
                                    viewModel = viewModel(
                                        ChooseExtraQuestionsViewModel::class.java,
                                        factory = ChooseExtraQuestionsViewModel.Companion.ChooseExtraQuestionsViewModelFactory(
                                            gameRepository = gameRepository,
                                            gameStateHandler = ChooseExtraQuestionsGameStateHandler(
                                                gameRepository = gameRepository,
                                                serverGameStateManager = serverGameStateManager,
                                            )
                                        )
                                    ), navController = navController
                                )
                            }
                            composable(Routes.Voting.route) {
                                VotingScreen(
                                    viewModel = viewModel(
                                        VotingViewModel::class.java,
                                        factory = VotingViewModel.Companion.VotingViewModelFactory(
                                            gameRepository = gameRepository,
                                            gameStateHandler = VotingGameStateHandler(
                                                gameRepository = gameRepository,
                                                serverGameStateManager = serverGameStateManager
                                            )
                                        )
                                    ), navController = navController
                                )
                            }
                            composable(Routes.VotingResults.route) {
                                VotingResultsScreen(
                                    viewModel = viewModel(
                                        VotingResultsViewModel::class.java,
                                        factory = VotingResultsViewModel.Companion.VotingResultsViewModelFactory(
                                            gameRepository = gameRepository,
                                            gameStateHandler = VotingResultsGameStateHandler(
                                                gameRepository = gameRepository,
                                                serverGameStateManager = serverGameStateManager
                                            )

                                        )
                                    ), navController = navController
                                )
                            }
                            composable(Routes.Scoreboard.route) {
                                ScoreScreen(
                                    viewModel = viewModel(
                                        ScoreViewModel::class.java,
                                        factory = ScoreViewModel.Companion.ScoreViewModelFactory(
                                            gameRepository = gameRepository,
                                            gameStateHandler = ScoreGameStateHandler(
                                                gameRepository = gameRepository,
                                                serverGameStateManager = serverGameStateManager
                                            )
                                        )
                                    ), navController = navController
                                )
                            }
                            composable(Routes.EndGame.route) {
                                EndGameScreen(
                                    viewModel = viewModel(
                                        EndGameViewModel::class.java,
                                        factory = EndGameViewModel.Companion.EndGameViewModelFactory(
                                            gameRepository = gameRepository, nsdHelper = nsdHelper
                                        )
                                    ), navController = navController
                                )
                            }
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
}