package com.example.nsddemo.ui.category_and_word

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.nsddemo.Debugging
import com.example.nsddemo.GameState
import com.example.nsddemo.ui.GameViewModel

@Composable
fun CategoryAndWordScreen(gameViewModel: GameViewModel, onNavigateToQuestionScreen: () -> Unit) {
    val gameState = gameViewModel.gameState.collectAsState()
    val currentGameState = gameState.value
    if (currentGameState is GameState.AskQuestion) {
        LaunchedEffect(Unit) {
            Log.d(Debugging.TAG, "Navigated to Question screen.")
            onNavigateToQuestionScreen()
        }
    }
    val isConfirmButtonPressedState =
        gameViewModel.isDisplayCategoryAndWordConfirmationSent.collectAsState()
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val category: String = remember {(currentGameState as GameState.DisplayCategoryAndWord).category}
        val word: String = remember {(currentGameState as GameState.DisplayCategoryAndWord).word}
        if (isConfirmButtonPressedState.value) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Waiting for all players to confirm...")
        } else {
            Text("Category: $category")
            Spacer(modifier = Modifier.height(16.dp))
            if (gameViewModel.isImposter!!) {
                Text("You are the imposter")
            } else {
                Text("Word: $word")
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = gameViewModel.onConfirmClick,
            enabled = !isConfirmButtonPressedState.value
        ) {
            Text("Confirm")
        }
    }
}