package com.example.nsddemo.presentation.screen.category_and_word

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.nsddemo.R
import com.example.nsddemo.core.util.Debugging.TAG
import com.example.nsddemo.core.util.GameState
import com.example.nsddemo.domain.util.Categories
import com.example.nsddemo.presentation.components.DefaultButton
import com.example.nsddemo.presentation.util.ConditionalComposable
import com.example.nsddemo.presentation.util.NavigationUtil.popBackStackAndNavigateTo
import com.example.nsddemo.presentation.util.UiEvent
import kotlinx.coroutines.flow.collectLatest

@Composable
fun CategoryAndWordScreen(viewModel: CategoryAndWordViewModel, navController: NavHostController) {
    LaunchedEffect(true) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is UiEvent.NavigateTo -> {
                    Log.d(TAG, "Category and Word Screen: Navigating to ${event.destination}")
                    navController.popBackStackAndNavigateTo(event.destination)
                }

                else -> {
                    // Do nothing
                }
            }
        }
    }
    val gameState = viewModel.gameState.collectAsState()
    val currentGameState = gameState.value
    if (currentGameState is GameState.AskQuestion) {
        LaunchedEffect(Unit) {
            viewModel.onEvent(CategoryAndWordEvent.StartQuestions)
        }
    }
    val isConfirmButtonPressedState = viewModel.state.collectAsState().value.isConfirmPressed
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ConditionalComposable(condition = isConfirmButtonPressedState, composable = {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                stringResource(R.string.waiting_for_all_players_to_confirm),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
        }, fallbackComposable = {
            Text(
                stringResource(
                    R.string.category,
                    stringResource(Categories.values()[viewModel.categoryOrdinal].nameResourceId)
                ),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                if (viewModel.isImposter) {
                    stringResource(R.string.you_are_the_imposter)
                } else {
                    stringResource(R.string.word, stringResource(viewModel.wordResourceId))
                },
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        })
        Spacer(Modifier.height(16.dp))
        DefaultButton(
            stringResource(R.string.confirm),
            onClick = { viewModel.onEvent(CategoryAndWordEvent.ConfirmCategoryAndWord) },
            enabled = !isConfirmButtonPressedState
        )
    }
}