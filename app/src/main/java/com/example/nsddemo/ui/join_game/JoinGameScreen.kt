package com.example.nsddemo.ui.join_game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.nsddemo.GameConstants
import com.example.nsddemo.R
import com.example.nsddemo.ui.GameViewModel
import com.example.nsddemo.ui.theme.englishTypography

@Composable
fun JoinGameScreen(
    gameViewModel: GameViewModel,
    joinGameViewModel: JoinGameViewModel,
    onJoinGamePressed: () -> Unit,
    onGoBackToMainMenuPressed: () -> Unit,
) {
    val gameCodeTextFieldEnabledState = remember { mutableStateOf(true) }
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
            value = joinGameViewModel.gameCodeTextFieldState.value,
            onValueChange = joinGameViewModel::onGameCodeTextFieldValueChange,
            textStyle = englishTypography.headlineMedium,
            enabled = gameCodeTextFieldEnabledState.value,
            onDonePressed = {
                gameCodeTextFieldEnabledState.value = false
                joinGameViewModel.onDiscoverAndResolveServicesClick()
                gameViewModel.onJoinGamePressed(joinGameViewModel.gameCode)
                onJoinGamePressed()
            }
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row {
            // TODO: This should cancel the discovery and resolve services
            Button(
                onClick = onGoBackToMainMenuPressed,
                enabled = gameCodeTextFieldEnabledState.value
            ) {
                Text(
                    stringResource(R.string.cancel),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Button(onClick = {
                gameCodeTextFieldEnabledState.value = false
                joinGameViewModel.onDiscoverAndResolveServicesClick()
                gameViewModel.onJoinGamePressed(joinGameViewModel.gameCode)
                onJoinGamePressed()
            }) {
                Text(
                    stringResource(R.string.join_game),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}