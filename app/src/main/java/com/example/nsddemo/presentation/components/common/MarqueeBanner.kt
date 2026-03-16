package com.example.nsddemo.presentation.components.common

import androidx.compose.foundation.MarqueeSpacing
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.nsddemo.presentation.components.modifier.brutalistBorderBottom
import com.example.nsddemo.presentation.theme.Dimens

@Composable
fun MarqueeBanner(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
) {
    // Note: We use .background() here instead of brutalistCard because banners are rectangular
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .brutalistBorderBottom(MaterialTheme.colorScheme.outline)
                .padding(vertical = Dimens.SpacingSmall),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            maxLines = 1,
            modifier =
                Modifier.basicMarquee(
                    iterations = Int.MAX_VALUE,
                    spacing = MarqueeSpacing(BrutalistMarqueeSpacing),
                ),
        )
    }
}

// Extra marquee settings
private val BrutalistMarqueeSpacing = Dimens.SpacingXLarge
