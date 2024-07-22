package com.example.nsddemo.presentation.screen.join_game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.nsddemo.R
import com.example.nsddemo.presentation.util.NavigationUtil.popBackStackAndNavigateTo
import com.example.nsddemo.presentation.util.UiEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

@Composable
fun JoinGameLoadingScreen(
    viewModel: JoinGameViewModel,
    navController: NavHostController,
    showSnackBar: (String) -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(true) {
        // TODO: Update all to collectLatest
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is UiEvent.NavigateTo -> {
                    navController.popBackStackAndNavigateTo(event.destination)
                }

                is UiEvent.ShowSnackBar -> {
                    showSnackBar(context.getString(event.messageResId))
                }
            }
        }
    }
    val hasFoundGame = viewModel.hasFoundGame.collectAsState(false).value
    if (hasFoundGame) {
        LaunchedEffect(Unit) {
            viewModel.onEvent(JoinGameEvent.GameFound)
        }
    }
    val hasJoinedGame = viewModel.hasJoinedGame.collectAsState(false).value
    if (hasJoinedGame) {
        LaunchedEffect(Unit) {
            viewModel.onEvent(JoinGameEvent.GameStarted)
        }
    }
    LaunchedEffect(Unit) {
        delay(5000L)
        if (!hasFoundGame) {
            viewModel.onEvent(JoinGameEvent.GameSearchTimedOut)
        }
    }
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            stringResource(R.string.finding_game),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        CircularProgressIndicator()
    }

}