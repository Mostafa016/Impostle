package com.example.nsddemo.presentation.screen.joingame.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.nsddemo.R
import com.example.nsddemo.presentation.components.common.BrutalistButton

@Composable
fun JoinGameActions(
    isJoinEnabled: Boolean,
    onJoinClick: () -> Unit,
    onCancelClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        BrutalistButton(
            text = stringResource(R.string.join_game),
            icon = Icons.Default.Add,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            enabled = isJoinEnabled,
            onClick = onJoinClick,
        )

        BrutalistButton(
            text = stringResource(R.string.cancel),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            onClick = onCancelClick,
        )
    }
}
