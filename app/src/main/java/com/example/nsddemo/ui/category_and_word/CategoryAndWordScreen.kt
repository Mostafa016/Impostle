package com.example.nsddemo.ui.category_and_word

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.nsddemo.Categories
import com.example.nsddemo.GameState
import com.example.nsddemo.R
import com.example.nsddemo.ui.GameViewModel

@Composable
fun CategoryAndWordScreen(gameViewModel: GameViewModel, onNavigateToQuestionScreen: () -> Unit) {
    val gameState = gameViewModel.gameState.collectAsState()
    val currentGameState = gameState.value
    if (currentGameState is GameState.AskQuestion) {
        LaunchedEffect(Unit) {
            onNavigateToQuestionScreen()
        }
    }
    val isConfirmButtonPressedState = gameViewModel.isConfirmButtonPressed.collectAsState().value
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val categoryOrdinal: Int =
            remember { (currentGameState as GameState.DisplayCategoryAndWord).categoryOrdinal }
        val wordResourceId: Int =
            remember { (currentGameState as GameState.DisplayCategoryAndWord).wordResourceId }
        if (isConfirmButtonPressedState) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                stringResource(R.string.waiting_for_all_players_to_confirm),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
        } else {
            Text(
                stringResource(
                    R.string.category,
                    stringResource(Categories.values()[categoryOrdinal].nameResourceId)
                ),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (gameViewModel.gameRepository.gameData.collectAsState().value.isImposter!!) {
                Text(
                    stringResource(R.string.you_are_the_imposter),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            } else {
                Text(
                    stringResource(R.string.word, stringResource(wordResourceId)),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = gameViewModel.onConfirmClick,
            enabled = !isConfirmButtonPressedState
        ) {
            Text(
                stringResource(R.string.confirm),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}