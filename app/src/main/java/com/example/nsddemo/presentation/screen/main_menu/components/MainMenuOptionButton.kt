package com.example.nsddemo.presentation.screen.main_menu.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight

@Composable
fun MainMenuOptionButton(
    textResID: Int,
    onClick: () -> Unit,
    enabled: Boolean
) {
    TextButton(onClick = onClick, enabled = enabled) {
        Text(
            stringResource(textResID),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}