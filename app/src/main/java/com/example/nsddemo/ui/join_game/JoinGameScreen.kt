package com.example.nsddemo.ui.join_game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.nsddemo.GameConstants
import com.example.nsddemo.R
import com.example.nsddemo.ui.theme.englishTypography

@Composable
fun JoinGameScreen(viewModel: JoinGameViewModel, onJoinGamePressed: () -> Unit) {
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
            value = viewModel.gameCodeTextFieldState.value,
            onValueChange = viewModel::onGameCodeTextFieldValueChange,
            textStyle = englishTypography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            viewModel.onDiscoverAndResolveServicesClick()
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