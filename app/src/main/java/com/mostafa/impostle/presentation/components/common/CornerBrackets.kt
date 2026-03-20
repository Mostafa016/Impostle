package com.mostafa.impostle.presentation.components.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.mostafa.impostle.presentation.theme.Dimens

@Composable
fun CornerBrackets(
    color: Color,
    modifier: Modifier = Modifier,
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(modifier = modifier.fillMaxSize()) {
            val stroke = Dimens.BorderThick
            val length = 48.dp
            val padding = Dimens.SpacingLarge

            // Bottom Left
            Canvas(
                modifier =
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(padding)
                        .size(length),
            ) {
                drawLine(
                    color,
                    start = Offset(0f, 0f),
                    end = Offset(0f, size.height),
                    strokeWidth = stroke.toPx(),
                )
                drawLine(
                    color,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = stroke.toPx(),
                )
            }
            // Bottom Right
            Canvas(
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(padding)
                        .size(length),
            ) {
                drawLine(
                    color,
                    start = Offset(size.width, 0f),
                    end = Offset(size.width, size.height),
                    strokeWidth = stroke.toPx(),
                )
                drawLine(
                    color,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = stroke.toPx(),
                )
            }
        }
    }
}
