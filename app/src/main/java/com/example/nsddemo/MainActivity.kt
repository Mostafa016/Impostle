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
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.nsddemo.ui.CategoryAndWordScreen
import com.example.nsddemo.ui.ChooseCategoryScreen
import com.example.nsddemo.ui.ClientGameStartLoadingScreen
import com.example.nsddemo.ui.CreateGameScreen
import com.example.nsddemo.ui.ExtraQuestionsScreen
import com.example.nsddemo.ui.JoinGameScreen
import com.example.nsddemo.ui.LobbyScreen
import com.example.nsddemo.ui.MainMenuScreen
import com.example.nsddemo.ui.QuestionScreen
import com.example.nsddemo.ui.ScoreScreen
import com.example.nsddemo.ui.TestViewModel
import com.example.nsddemo.ui.VotingResultsScreen
import com.example.nsddemo.ui.VotingScreen


class MainActivity : AppCompatActivity() {
    @Suppress("UNCHECKED_CAST")
    var factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TestViewModel(
                nsdManager = getSystemService(Context.NSD_SERVICE) as NsdManager,
                wifiManager = getSystemService(Context.WIFI_SERVICE) as WifiManager
            ) as T
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val testViewModel = ViewModelProvider(this, factory)[TestViewModel::class.java]

        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
            ) {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = Screen.MainMenu.route) {
                    composable(Screen.MainMenu.route) {
                        MainMenuScreen(testViewModel,
                            onNavigateToCreateGame = { navController.navigate(Screen.CreateGame.route) },
                            onNavigateToJoinGame = { navController.navigate(Screen.JoinGame.route) })
                    }
                    composable(Screen.CreateGame.route) {
                        CreateGameScreen(testViewModel,
                            onGameCreated = { navController.navigate(Screen.Lobby.route) })
                    }
                    composable(Screen.JoinGame.route) {
                        JoinGameScreen(testViewModel, onJoinGamePressed = {
                            //TODO: Handle failure to find game cases
                            navController.navigate(Screen.ClientGameStartLoading.route)
                        })
                    }
                    composable(Screen.ClientGameStartLoading.route) {
                        //TODO: Add joined players messages to all players so lobby has a meaning
                        // or just keep the loading until host starts game and navigate to askquestion
                        // screen
                        ClientGameStartLoadingScreen(testViewModel, onGameJoined = {
                            //TODO: Maybe wait for category and word message to arrive before
                            // navigating to CategoryAndWord screen
                            navController.navigate(Screen.CategoryAndWord.route)
                        })
                    }
                    composable(Screen.Lobby.route) {
                        LobbyScreen(testViewModel, onChooseCategoryClick = {
                            navController.navigate(Screen.ChooseCategory.route)
                        })
                    }
                    composable(Screen.ChooseCategory.route) {
                        ChooseCategoryScreen(testViewModel,
                            onCategoryCardClick = { category ->
                                testViewModel.chooseCategory(category)
                                testViewModel.onSendCategoryAndWordClick()
                                navController.navigate(Screen.CategoryAndWord.route)
                            })
                    }
                    composable(Screen.CategoryAndWord.route) {
                        CategoryAndWordScreen(testViewModel,
                            onNavigateToQuestionScreen = { navController.navigate(Screen.Question.route) })
                    }
                    composable(Screen.Question.route) {
                        //TODO: Question screen should get it's stuff from viewmodel and gamestate
                        QuestionScreen(testViewModel,
                            onNavigateToExtraQuestionsScreen = { navController.navigate(Screen.ExtraQuestions.route) })
                    }
                    composable(Screen.ExtraQuestions.route) {
                        ExtraQuestionsScreen(testViewModel,
                            onNavigateToVotingScreen = { navController.navigate(Screen.Voting.route) })
                    }
                    composable(Screen.Voting.route) {
                        VotingScreen(testViewModel,
                            onNavigateToVotingResultsScreen = { navController.navigate(Screen.VotingResults.route) })
                    }
                    composable(Screen.VotingResults.route) {
                        VotingResultsScreen(testViewModel, onShowScoreClick = {
                            navController.navigate(Screen.Scoreboard.route)
                            testViewModel.onShowScoreClick()
                        })
                    }
                    composable(Screen.Scoreboard.route) {
                        ScoreScreen(testViewModel,
                            onPlayAgainPress = testViewModel.onReplayClick,
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