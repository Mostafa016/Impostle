package com.mostafa.impostle.presentation.components.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mostafa.impostle.presentation.components.modifier.brutalistCard
import com.mostafa.impostle.presentation.theme.Dimens

@Composable
fun BrutalistStatusLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .rotate(2f)
                .brutalistCard(
                    backgroundColor = Color.Black,
                    borderColor = Color.Black,
                    cornerRadius = 4.dp,
                    shadowOffset = Dimens.ShadowMedium,
                    borderWidth = 0.dp,
                ).padding(horizontal = 24.dp, vertical = 12.dp),
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            letterSpacing = 2.sp,
        )
    }
}
