package com.example.nsddemo.presentation.screen.main_menu.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.nsddemo.presentation.components.modifier.brutalistCard

@Composable
fun HeroTitleCard(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .brutalistCard(
                backgroundColor = MaterialTheme.colorScheme.surface,
                borderColor = MaterialTheme.colorScheme.outline,
                cornerRadius = 16.dp,
                shadowOffset = 4.dp
            )
            .padding(vertical = 32.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurface)) {
                    append("IMPOST")
                }
                withStyle(SpanStyle(color = MaterialTheme.colorScheme.secondary)) {
                    append("LE")
                }
            },
            style = MaterialTheme.typography.displayLarge
        )

        HorizontalDivider(
            thickness = 4.dp,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .padding(vertical = 12.dp)
                .clip(RoundedCornerShape(2.dp))
        )

        Text(
            text = "SOCIAL DEDUCTION PROTOCOL",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}
