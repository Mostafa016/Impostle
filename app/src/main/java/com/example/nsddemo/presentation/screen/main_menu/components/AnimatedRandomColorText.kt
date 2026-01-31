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
import androidx.compose.ui.text.TextStyle
import com.example.nsddemo.domain.model.NewPlayerColors
import com.example.nsddemo.presentation.util.toComposeColor

@Composable
fun AnimatedRandomColorText(text: String, style: TextStyle) {
    val titleTransition = rememberInfiniteTransition(label = "title")
    val titleAnimationInitialColor =
        remember { NewPlayerColors.entries.map { it.toComposeColor() }.random() }
    val titleAnimationTargetColor =
        remember {
            NewPlayerColors.entries.map { it.toComposeColor() }
                .filter { it != titleAnimationInitialColor }.random()
        }
    val titleColor by titleTransition.animateColor(
        initialValue = titleAnimationInitialColor,
        targetValue = titleAnimationTargetColor,
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