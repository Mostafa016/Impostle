package com.example.nsddemo.presentation.screen.settings.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.nsddemo.presentation.theme.Dimens

@Composable
fun BrutalistToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val trackColor =
        if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val thumbColor =
        if (checked) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.outline
    val borderColor = MaterialTheme.colorScheme.outline

    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 32.dp else 2.dp,
        label = "thumb_animation",
    )

    Box(
        modifier =
            modifier
                .width(64.dp)
                .height(32.dp)
                .clip(RoundedCornerShape(50))
                .background(trackColor)
                .border(Dimens.BorderThin, borderColor, RoundedCornerShape(50))
                .clickable { onCheckedChange(!checked) },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier =
                Modifier
                    .offset(x = thumbOffset)
                    .size(28.dp)
                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                    .border(Dimens.BorderThin, borderColor, CircleShape)
                    .padding(4.dp),
        ) {
            if (checked) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(thumbColor, CircleShape),
                )
            }
        }

        if (checked) {
            Text(
                text = "ON",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(start = 10.dp),
            )
        } else {
            Text(
                text = "OFF",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier =
                    Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 10.dp),
            )
        }
    }
}
