package com.example.nsddemo.presentation.screen.imposterguess.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.nsddemo.presentation.components.modifier.brutalistCard
import com.example.nsddemo.presentation.theme.Dimens

@Composable
fun ImposterGuessWordSelectionCard(
    word: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val outlineColor = MaterialTheme.colorScheme.outline
    val containerColor =
        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val contentColor =
        if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val shadow = if (isSelected) Dimens.ShadowSmall else Dimens.ShadowMedium

    Box(
        modifier =
            modifier
                .aspectRatio(1f)
                .brutalistCard(
                    backgroundColor = containerColor,
                    borderColor = if (isSelected) MaterialTheme.colorScheme.primary else outlineColor,
                    shadowOffset = shadow,
                    cornerRadius = Dimens.CornerLarge,
                    borderWidth = if (isSelected) Dimens.BorderThick else Dimens.BorderThin,
                ).clickable { onClick() }
                .padding(Dimens.SpacingMedium),
    ) {
        if (isSelected) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .size(20.dp)
                        .background(MaterialTheme.colorScheme.onPrimary, CircleShape)
                        .border(1.dp, Color.Black, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = word.uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = contentColor,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}
