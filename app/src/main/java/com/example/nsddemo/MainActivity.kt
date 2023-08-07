package com.example.nsddemo

import android.content.Context
import android.net.nsd.NsdManager
import android.net.wifi.WifiManager
import android.os.Bundle
import android.widget.*
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.nsddemo.ui.category_and_word.CategoryAndWordScreen
import com.example.nsddemo.ui.category_and_word.ChooseCategoryScreen
import com.example.nsddemo.ui.category_and_word.ChooseCategoryViewModel
import com.example.nsddemo.ui.game_loading.JoinGameLoadingScreen
import com.example.nsddemo.ui.game_loading.CreateGameLoadingScreen
import com.example.nsddemo.ui.question.ExtraQuestionsScreen
import com.example.nsddemo.ui.join_game.JoinGameScreen
import com.example.nsddemo.ui.lobby.LobbyScreen
import com.example.nsddemo.ui.main_menu.MainMenuScreen
import com.example.nsddemo.ui.main_menu.MainMenuViewModel
import com.example.nsddemo.ui.question.QuestionScreen
import com.example.nsddemo.ui.score.ScoreScreen
import com.example.nsddemo.ui.GameViewModel
import com.example.nsddemo.ui.voting_results.VotingResultsScreen
import com.example.nsddemo.ui.voting.VotingScreen
import com.example.nsddemo.ui.join_game.JoinGameViewModel


class MainActivity : AppCompatActivity() {
    @Suppress("UNCHECKED_CAST")
    val factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GameViewModel(
                nsdManager = getSystemService(Context.NSD_SERVICE) as NsdManager,
                wifiManager = getSystemService(Context.WIFI_SERVICE) as WifiManager
            ) as T
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val gameViewModel = ViewModelProvider(this, factory)[GameViewModel::class.java]
        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
            ) {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = Screen.MainMenu.route) {
                    composable(Screen.MainMenu.route) {
                        MainMenuScreen(
                            gameViewModel,
                            viewModel(
                                MainMenuViewModel::class.java,
                                factory = MainMenuViewModel.Companion.MainMenuViewModelFactory(
                                    gameViewModel
                                )
                            ),
                            onNavigateToCreateGame = { navController.navigate(Screen.CreateGame.route) },
                            onNavigateToJoinGame = { navController.navigate(Screen.JoinGame.route) })
                    }
                    composable(Screen.CreateGame.route) {
                        CreateGameLoadingScreen(gameViewModel,
                            onGameCreated = { navController.navigate(Screen.Lobby.route) })
                    }
                    composable(Screen.JoinGame.route) {
                        JoinGameScreen(viewModel(
                            JoinGameViewModel::class.java,
                            factory = JoinGameViewModel.Companion.JoinGameViewModelFactory(
                                gameViewModel,
                                getSystemService(Context.NSD_SERVICE) as NsdManager
                            )
                        ), onJoinGamePressed = {
                            //TODO: Handle failure to find game cases
                            navController.navigate(Screen.ClientGameStartLoading.route)
                        })
                    }
                    composable(Screen.ClientGameStartLoading.route) {
                        //TODO: Add joined players messages to all players so lobby has a meaning
                        // or just keep the loading until host starts game and navigate to askquestion
                        // screen
                        JoinGameLoadingScreen(gameViewModel, onGameJoined = {
                            navController.navigate(Screen.CategoryAndWord.route)
                        })
                    }
                    composable(Screen.Lobby.route) {
                        LobbyScreen(gameViewModel, onChooseCategoryClick = {
                            navController.navigate(Screen.ChooseCategory.route)
                        })
                    }
                    composable(Screen.ChooseCategory.route) {
                        ChooseCategoryScreen(vm = viewModel(
                            ChooseCategoryViewModel::class.java,
                            factory = ChooseCategoryViewModel.Companion.ChooseCategoryViewModelFactory(
                                gameViewModel
                            )
                        ),
                            onNavigateToCategoryAndWord = { navController.navigate(Screen.CategoryAndWord.route) })
                    }
                    composable(Screen.CategoryAndWord.route) {
                        CategoryAndWordScreen(gameViewModel,
                            onNavigateToQuestionScreen = { navController.navigate(Screen.Question.route) })
                    }
                    composable(Screen.Question.route) {
                        QuestionScreen(gameViewModel,
                            onNavigateToExtraQuestionsScreen = { navController.navigate(Screen.ExtraQuestions.route) })
                    }
                    composable(Screen.ExtraQuestions.route) {
                        ExtraQuestionsScreen(gameViewModel,
                            onNavigateToVotingScreen = { navController.navigate(Screen.Voting.route) })
                    }
                    composable(Screen.Voting.route) {
                        VotingScreen(gameViewModel,
                            onNavigateToVotingResultsScreen = { navController.navigate(Screen.VotingResults.route) })
                    }
                    composable(Screen.VotingResults.route) {
                        VotingResultsScreen(gameViewModel, onShowScoreClick = {
                            navController.navigate(Screen.Scoreboard.route)
                            gameViewModel.onShowScoreClick()
                        })
                    }
                    composable(Screen.Scoreboard.route) {
                        ScoreScreen(gameViewModel,
                            onPlayAgainPress = gameViewModel.onReplayClick,
                            onNavigateToLobbyScreen = { navController.navigate(Screen.Lobby.route) },
                            onNavigateToJoinGameScreen = { navController.navigate(Screen.ClientGameStartLoading.route) })
                    }
                }
            }
        }
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


}