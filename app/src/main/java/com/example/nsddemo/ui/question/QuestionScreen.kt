package com.example.nsddemo.ui.question

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nsddemo.Debugging.TAG
import com.example.nsddemo.GameState
import com.example.nsddemo.ui.GameViewModel


@Composable
fun QuestionScreen(
    viewModel: GameViewModel, onNavigateToExtraQuestionsScreen: () -> Unit
) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val gameState = viewModel.gameState.collectAsState()
        if (gameState.value is GameState.AskExtraQuestions) {
            LaunchedEffect(Unit) {
                Log.d(TAG, "Navigated to ExtraQuestions screen.")
                onNavigateToExtraQuestionsScreen()
            }
        } else if (gameState.value is GameState.AskQuestion) {
            val currentGameState = gameState.value as GameState.AskQuestion
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Text(
                    if (currentGameState.isAsking) "You" else currentGameState.asker.name,
                    style = TextStyle(
                        color = Color(currentGameState.asker.color.toLong(radix = 16)),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 24.sp
                    )
                )
                Text(
                    if (currentGameState.isAsking) " are asking " else " is asking ",
                    style = TextStyle(
                        fontSize = 24.sp
                    )
                )
                Text(
                    currentGameState.asked.name,
                    style = TextStyle(
                        color = Color(currentGameState.asked.color.toLong(radix = 16)),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 24.sp
                    )
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (currentGameState.isAsking) {
                Button(
                    onClick = viewModel.onDoneClick,
                    enabled = !viewModel.isQuestionDone.collectAsState().value
                ) {
                    Text("Done", style = TextStyle(fontSize = 24.sp))
                }
            }
        }
    }
}