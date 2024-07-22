package com.example.nsddemo.presentation.components

import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

@Composable
fun DefaultButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    style: TextStyle = TextStyle.Default,
    autoDisable: Boolean = true,
) {
    val isButtonEnabled = remember { mutableStateOf(enabled) }
    Button(
        onClick = onClick,
        enabled = if (autoDisable) isButtonEnabled.value else enabled,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold) + style
        )
    }
}