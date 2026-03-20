package com.mostafa.impostle.presentation.screen.pause.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mostafa.impostle.R
import com.mostafa.impostle.presentation.components.common.BrutalistButton

@Composable
fun HostPauseControls(
    isEndGameEnabled: Boolean,
    onResume: () -> Unit,
    onGameEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BrutalistButton(
            text = stringResource(R.string.continue_anyway),
            icon = Icons.Default.PlayArrow,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            enabled = true,
            onClick = onResume,
        )

        BrutalistButton(
            text = stringResource(R.string.end_game),
            icon = Icons.Default.Close,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.error,
            enabled = isEndGameEnabled,
            onClick = onGameEnd,
        )
    }
}
