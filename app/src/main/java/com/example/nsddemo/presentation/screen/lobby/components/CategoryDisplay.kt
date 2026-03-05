package com.example.nsddemo.presentation.screen.lobby.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.sharp.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.nsddemo.presentation.components.modifier.brutalistCard
import com.example.nsddemo.presentation.util.UiCategory

@Composable
fun CategoryDisplay(
    chosenCategory: UiCategory?,
    isHost: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor =
        if (isHost) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val contentColor =
        if (isHost) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val shadow = if (isHost) 4.dp else 0.dp
    val iconTint =
        if (isHost) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(
            alpha = 0.5f
        )
    val iconBoxColor =
        if (isHost) Color.Black.copy(alpha = 0.1f) else MaterialTheme.colorScheme.background.copy(
            alpha = 0.7f
        )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .brutalistCard(
                backgroundColor = backgroundColor,
                borderColor = MaterialTheme.colorScheme.outline,
                shadowOffset = shadow
            )
            .clickable(enabled = isHost, onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(iconBoxColor, RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Sharp.Star,
                    contentDescription = null,
                    tint = iconTint
                )
            }
            Column {
                Text(
                    text = "CATEGORY",
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.7f)
                )
                Text(
                    text = chosenCategory?.nameResId?.let { stringResource(it) }?.uppercase()
                        ?: "SELECT...",
                    style = MaterialTheme.typography.titleLarge,
                    color = contentColor
                )
            }
        }

        Icon(
            imageVector = if (isHost) Icons.Default.Edit else Icons.Default.Lock,
            contentDescription = null,
            tint = iconTint
        )
    }
}
