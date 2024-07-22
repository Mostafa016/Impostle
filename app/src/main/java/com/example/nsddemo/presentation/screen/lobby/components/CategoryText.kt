package com.example.nsddemo.presentation.screen.lobby.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.nsddemo.R

@Composable
fun CategoryText(text: String?, modifier: Modifier = Modifier) {
    Text(
        stringResource(
            R.string.category, text ?: stringResource(
                R.string.no_category_chosen
            )
        ),
        modifier = modifier,
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onBackground,
    )
}