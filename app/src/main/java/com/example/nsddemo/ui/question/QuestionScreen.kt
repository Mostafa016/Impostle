package com.example.nsddemo.ui.question

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.nsddemo.Categories
import com.example.nsddemo.GameState
import com.example.nsddemo.R
import com.example.nsddemo.ui.GameViewModel
import com.example.nsddemo.ui.theme.englishTypography
import kotlinx.coroutines.flow.map


@OptIn(ExperimentalAnimationApi::class)
@Composable
fun QuestionScreen(
    viewModel: GameViewModel, onNavigateToChooseExtraQuestionsScreen: () -> Unit
) {
    val currentPlayerState = viewModel.gameRepository.gameData.map {
        it.currentPlayer
    }.collectAsState(null)
    if (viewModel.wordDialogVisibilityState.value) {
        Dialog(onDismissRequest = { viewModel.onWordDialogOkClicked() }) {
            Column(
                Modifier.background(MaterialTheme.colorScheme.background),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(
                        R.string.category,
                        stringResource(Categories.values()[viewModel.gameRepository.gameData.collectAsState().value.categoryOrdinal].nameResourceId)
                    ),
                    Modifier.padding(4.dp),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = if (viewModel.gameRepository.gameData.collectAsState().value.isImposter!!) stringResource(
                        R.string.you_are_the_imposter
                    )
                    else stringResource(
                        R.string.word,
                        stringResource(viewModel.gameRepository.gameData.collectAsState().value.wordResID)
                    ),
                    Modifier.padding(4.dp),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(8.dp))
                Divider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .width(1.dp)
                        .padding(horizontal = 4.dp),
                )
                TextButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    onClick = { viewModel.onWordDialogOkClicked() },
                ) {
                    Text(stringResource(R.string.ok), style = MaterialTheme.typography.titleLarge)
                }
            }
        }
    }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.align(Alignment.Start)) {
            TextButton(onClick = { viewModel.onShowWordClick() }) {
                Text(
                    stringResource(R.string.show_word),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val gameState = viewModel.gameState.collectAsState()
            if (gameState.value is GameState.ChooseExtraQuestions) {
                LaunchedEffect(Unit) {
                    onNavigateToChooseExtraQuestionsScreen()
                }
            } else if (gameState.value is GameState.AskQuestion) {
                val currentGameState = gameState.value as GameState.AskQuestion
                @Suppress("AnimatedContentLabel") AnimatedContent(targetState = currentGameState,
                    transitionSpec = {
                        slideInHorizontally(
                            animationSpec = tween(
                                500,
                                easing = EaseIn
                            )
                        ) with ExitTransition.None
                    }) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        Text(
                            if (currentGameState.isAsking) stringResource(R.string.you) else currentGameState.asker.name,
                            style = englishTypography.headlineLarge,
                            color = Color(currentGameState.asker.color.toLong(radix = 16)),
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            if (currentGameState.isAsking) stringResource(R.string.are_asking)
                            else {
                                if (currentGameState.asked != currentPlayerState.value) {
                                    stringResource(R.string.is_asking)
                                } else {
                                    stringResource(R.string.is_asking_you)
                                }
                            },
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            if (currentGameState.asked != currentPlayerState.value) currentGameState.asked.name
                            else stringResource(R.string.you_asked),
                            style = englishTypography.headlineLarge,
                            color = Color(currentGameState.asked.color.toLong(radix = 16)),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                if (currentGameState.isAsking) {
                    Button(
                        onClick = viewModel.onDoneClick,
                        enabled = !viewModel.isQuestionDone.collectAsState().value
                    ) {
                        Text(
                            stringResource(R.string.done),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}