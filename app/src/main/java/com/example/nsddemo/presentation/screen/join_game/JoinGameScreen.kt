package com.example.nsddemo.presentation.screen.join_game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.nsddemo.R
import com.example.nsddemo.core.util.GameConstants
import com.example.nsddemo.presentation.components.DefaultButton
import com.example.nsddemo.presentation.screen.join_game.components.GameCodeTextField
import com.example.nsddemo.presentation.theme.englishTypography
import com.example.nsddemo.presentation.util.NavigationUtil.popBackStackAndNavigateTo
import com.example.nsddemo.presentation.util.UiEvent
import kotlinx.coroutines.flow.collectLatest

@Composable
fun JoinGameScreen(
    viewModel: JoinGameViewModel,
    navController: NavHostController,
) {
    LaunchedEffect(true) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is UiEvent.NavigateTo -> {
                    navController.popBackStackAndNavigateTo(event.destination)
                }

                else -> {
                    // Do nothing
                }
            }
        }
    }
    val state = viewModel.state.collectAsState().value
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            stringResource(R.string.enter_game_code),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(16.dp))
        GameCodeTextField(
            codeLength = GameConstants.CODE_LENGTH,
            value = state.gameCodeTextFieldText,
            onValueChange = { viewModel.onEvent(JoinGameEvent.GameCodeTextFieldValueChange(it)) },
            textStyle = englishTypography.headlineMedium,
            enabled = state.gameCodeTextFieldEnabled,
            onDonePressed = {
                viewModel.onEvent(JoinGameEvent.JoinGame)
            }
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row {
            DefaultButton(
                stringResource(R.string.cancel),
                onClick = { viewModel.onEvent(JoinGameEvent.GoBackToMainMenu) },
            )
            Spacer(modifier = Modifier.width(4.dp))
            DefaultButton(
                stringResource(R.string.join_game),
                onClick = { viewModel.onEvent(JoinGameEvent.JoinGame) },
                enabled = state.gameCodeTextFieldText.length == GameConstants.CODE_LENGTH,
            )
        }
    }
}