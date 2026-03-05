package com.example.nsddemo.presentation.screen.role_reveal.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nsddemo.presentation.components.common.BrutalistDashedDivider
import com.example.nsddemo.presentation.components.modifier.brutalistCard
import com.example.nsddemo.presentation.theme.BrutalistDimens

@Composable
fun InnocentRoleCard(
    category: String,
    secretWord: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = BrutalistDimens.SpacingLarge,
                vertical = BrutalistDimens.SpacingXLarge
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        RoleBadge(
            text = "YOU ARE INNOCENT",
            backgroundColor = MaterialTheme.colorScheme.secondary,
            textColor = MaterialTheme.colorScheme.onSecondary
        )

        CategoryInfo(category)

        // Secret Word Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .brutalistCard(
                    backgroundColor = MaterialTheme.colorScheme.primary,
                    borderColor = MaterialTheme.colorScheme.outline,
                    shadowOffset = BrutalistDimens.ShadowSmall,
                    borderWidth = BrutalistDimens.BorderThick
                )
                .padding(BrutalistDimens.SpacingXLarge),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = secretWord.uppercase(),
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 36.sp),
                color = MaterialTheme.colorScheme.onPrimary,
                textAlign = TextAlign.Center,
                lineHeight = 40.sp
            )
        }

        BrutalistDashedDivider()

        Text(
            text = "YOUR GOAL:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Figure out who the Imposter is without revealing the secret word.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ImposterRoleCard(
    category: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = BrutalistDimens.SpacingLarge,
                vertical = BrutalistDimens.SpacingXLarge
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        RoleBadge(
            text = "YOU ARE THE IMPOSTER",
            backgroundColor = MaterialTheme.colorScheme.primary,
            textColor = MaterialTheme.colorScheme.onPrimary
        )

        CategoryInfo(category)

        // Mystery Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .brutalistCard(
                    backgroundColor = MaterialTheme.colorScheme.primary,
                    borderColor = MaterialTheme.colorScheme.outline,
                    shadowOffset = BrutalistDimens.ShadowMedium,
                    borderWidth = BrutalistDimens.BorderThick
                )
                .padding(BrutalistDimens.SpacingXLarge),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "SECRET\nIDENTITY",
                    style = MaterialTheme.typography.displayMedium.copy(fontSize = 32.sp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    textAlign = TextAlign.Center,
                    lineHeight = 36.sp
                )
            }
        }

        BrutalistDashedDivider()

        Text(
            text = "YOUR GOAL:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Figure out the secret word everyone else knows without revealing your identity.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun RoleBadge(text: String, backgroundColor: Color, textColor: Color) {
    Box(
        modifier = Modifier
            .offset(y = -BrutalistDimens.SpacingXLarge - 4.dp)
            .brutalistCard(
                backgroundColor = backgroundColor,
                borderColor = MaterialTheme.colorScheme.outline,
                cornerRadius = BrutalistDimens.CornerPill,
                borderWidth = BrutalistDimens.BorderThin,
                shadowOffset = BrutalistDimens.ShadowSmall
            )
            .padding(horizontal = 24.dp, vertical = 8.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = textColor
        )
    }
}

@Composable
private fun CategoryInfo(category: String) {
    Text(
        text = "CATEGORY",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    )
    Text(
        text = category.uppercase(),
        style = MaterialTheme.typography.headlineLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(bottom = BrutalistDimens.SpacingLarge)
    )
}
