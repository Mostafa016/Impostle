package com.example.nsddemo.presentation.screen.main_menu.components

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.example.nsddemo.presentation.util.PlayerColors

@Composable
fun AnimatedRandomColorText(text: String, style: TextStyle) {
    val titleTransition = rememberInfiniteTransition(label = "title")
    val titleAnimationInitialColor = remember { PlayerColors.values().random() }
    val titleAnimationTargetColor =
        remember { PlayerColors.values().filter { it != titleAnimationInitialColor }.random() }
    val titleColor by titleTransition.animateColor(
        initialValue = Color(titleAnimationInitialColor.argb),
        targetValue = Color(titleAnimationTargetColor.argb),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5000, delayMillis = 1000),
            repeatMode = RepeatMode.Reverse
        ), label = "titleColor"
    )
    Text(
        text,
        style = style.copy(color = titleColor),
    )
}