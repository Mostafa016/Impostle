package com.example.nsddemo.presentation.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle

@Composable
fun ListTitleText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default
) {
    Text(
        text,
        modifier = modifier,
        style = MaterialTheme.typography.headlineLarge + style,
        color = MaterialTheme.colorScheme.onBackground
    )
}