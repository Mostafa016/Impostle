package com.example.nsddemo.presentation.components.modifier

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.nsddemo.presentation.theme.BrutalistDimens

/**
 * Applies the signature "Friendly Cyber-Brutalist" look:
 * 1. Hard Drop Shadow (drawn behind)
 * 2. Background Color
 * 3. Thick Border
 * 4. Rounded Corners
 */
fun Modifier.brutalistCard(
    backgroundColor: Color,
    borderColor: Color,
    shadowColor: Color = Color.Black, // Usually Black, even in Dark Mode
    cornerRadius: Dp = BrutalistDimens.CornerLarge,
    borderWidth: Dp = BrutalistDimens.BorderThin,
    shadowOffset: Dp = BrutalistDimens.ShadowMedium,
) = this
    .drawBehind {
        if (shadowOffset > 0.dp) {
            drawRoundRect(
                color = shadowColor,
                topLeft = Offset(shadowOffset.toPx(), shadowOffset.toPx()),
                size = size,
                cornerRadius = CornerRadius(cornerRadius.toPx())
            )
        }
    }
    .clip(RoundedCornerShape(cornerRadius))
    .background(backgroundColor)
    .border(borderWidth, borderColor, RoundedCornerShape(cornerRadius))

/**
 * Draws the infinite grid background.
 */
fun Modifier.brutalistGridBackground(
    backgroundColor: Color,
    gridLineColor: Color,
    gridSize: Dp = 40.dp
) = this
    .background(backgroundColor)
    .drawBehind {
        val step = gridSize.toPx()
        val stroke = 1f // Keep grid lines thin

        // Vertical Lines
        for (x in 0..size.width.toInt() step step.toInt()) {
            drawLine(
                gridLineColor,
                start = Offset(x.toFloat(), 0f),
                end = Offset(x.toFloat(), size.height),
                strokeWidth = stroke
            )
        }
        // Horizontal Lines
        for (y in 0..size.height.toInt() step step.toInt()) {
            drawLine(
                gridLineColor,
                start = Offset(0f, y.toFloat()),
                end = Offset(size.width, y.toFloat()),
                strokeWidth = stroke
            )
        }
    }

/**
 * Draws a simple bottom border (used for Marquee).
 */
fun Modifier.brutalistBorderBottom(
    color: Color,
    strokeWidth: Dp = BrutalistDimens.BorderThin
) = drawBehind {
    drawLine(
        color = color,
        start = Offset(0f, size.height),
        end = Offset(size.width, size.height),
        strokeWidth = strokeWidth.toPx()
    )
}