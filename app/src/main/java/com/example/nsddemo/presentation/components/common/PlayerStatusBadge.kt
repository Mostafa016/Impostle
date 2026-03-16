package com.example.nsddemo.presentation.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.example.nsddemo.R
import com.example.nsddemo.presentation.theme.Dimens

/**
 * Standardized status indicator for player ready states.
 */
@Composable
fun PlayerStatusBadge(
    isReady: Boolean,
    modifier: Modifier = Modifier,
) {
    val bgColor =
        when {
            isReady -> Color(0xFF22C55E) // Green
            else -> Color(0xFFE5E7EB) // Gray
        }

    val iconColor = if (isReady) Color.White else Color.Gray

    Box(
        modifier =
            modifier
                .size(36.dp)
                .background(bgColor, CircleShape)
                .border(Dimens.BorderThin, Color.Black, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (isReady) Icons.Default.Check else ImageVector.vectorResource(R.drawable.sharp_hourglass_24),
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(20.dp),
        )
    }
}
