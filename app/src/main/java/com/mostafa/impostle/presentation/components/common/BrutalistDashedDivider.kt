package com.mostafa.impostle.presentation.components.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.unit.dp

@Composable
fun BrutalistDashedDivider(modifier: Modifier = Modifier) {
    val dividerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    Canvas(
        modifier =
            modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(vertical = 24.dp),
    ) {
        drawLine(
            color = dividerColor,
            start = Offset(0f, size.height / 2),
            end = Offset(size.width, size.height / 2),
            strokeWidth = 2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f),
        )
    }
}
