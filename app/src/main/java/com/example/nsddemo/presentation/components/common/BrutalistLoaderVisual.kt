package com.example.nsddemo.presentation.components.common

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.example.nsddemo.R

@Composable
fun BrutalistLoaderVisual(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_spin")

    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "pulse",
    )

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(3000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "spin",
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier =
                Modifier
                    .size(140.dp)
                    .scale(scale)
                    .background(
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                        CircleShape,
                    ),
        )

        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.sharp_visibility_24),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground,
            modifier =
                Modifier
                    .size(80.dp)
                    .rotate(rotation),
        )
    }
}
