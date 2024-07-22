package com.example.nsddemo.presentation.screen.question.components

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight

@Composable
fun ShowWordButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(modifier = modifier) {
        TextButton(onClick = onClick) {
            Text(
                text,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}