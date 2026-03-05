package com.example.nsddemo.presentation.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Standardized badge for player rankings with color coding.
 */
@Composable
fun RankBadge(
    rank: Int,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = when (rank) {
        1 -> Color(0xFFFACC15) to Color.Black // Gold
        2 -> Color(0xFFE5E7EB) to Color.Black // Silver
        3 -> Color(0xFFD97706) to Color.White // Bronze
        else -> Color.Transparent to Color.Gray
    }

    val borderStroke = if (rank <= 3) 2.dp else 1.dp

    Box(
        modifier = modifier
            .size(32.dp)
            .background(bgColor, CircleShape)
            .border(borderStroke, MaterialTheme.colorScheme.outline, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = rank.toString(),
            style = MaterialTheme.typography.titleSmall,
            color = textColor,
            fontWeight = FontWeight.Bold
        )
    }
}
