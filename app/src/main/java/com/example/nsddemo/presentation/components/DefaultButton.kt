package com.example.nsddemo.presentation.components

import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

// TODO: Make the button autoDisable after first click to disallow duplicate pressing
@Composable
fun DefaultButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    style: TextStyle = TextStyle.Default,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold) + style
        )
    }
}