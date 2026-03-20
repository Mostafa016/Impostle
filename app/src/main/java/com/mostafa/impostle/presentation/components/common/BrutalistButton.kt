package com.mostafa.impostle.presentation.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.mostafa.impostle.presentation.components.modifier.brutalistCard
import com.mostafa.impostle.presentation.theme.Dimens

@Composable
fun BrutalistButton(
    text: String,
    subtext: String? = null,
    icon: ImageVector? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    // Variants
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    borderColor: Color = MaterialTheme.colorScheme.outline,
) {
    // Logic: If disabled, gray out everything
    val finalContainer = if (enabled) containerColor else MaterialTheme.colorScheme.surfaceVariant
    val finalContent =
        if (enabled) contentColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    val finalBorder =
        if (enabled) borderColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .brutalistCard(
                    backgroundColor = finalContainer,
                    borderColor = finalBorder,
                    cornerRadius = Dimens.CornerLarge,
                    borderWidth = if (enabled) Dimens.BorderThick else Dimens.BorderThin,
                ).clickable(enabled = enabled, onClick = onClick)
                .padding(Dimens.SpacingMedium),
        horizontalArrangement = if (icon == null) Arrangement.Center else Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.Center) {
            Text(
                text = text.uppercase(),
                style = MaterialTheme.typography.titleLarge,
                color = finalContent,
            )
            if (subtext != null) {
                Text(
                    text = subtext,
                    style = MaterialTheme.typography.labelSmall,
                    color = finalContent.copy(alpha = 0.8f),
                )
            }
        }

        if (icon != null) {
            // The semi-transparent box around the icon
            Box(
                modifier =
                    Modifier
                        .size(40.dp)
                        .background(
                            color =
                                if (containerColor == MaterialTheme.colorScheme.primary) {
                                    Color.White.copy(alpha = 0.2f)
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                },
                            shape = RoundedCornerShape(Dimens.CornerMedium),
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = finalContent,
                )
            }
        }
    }
}
