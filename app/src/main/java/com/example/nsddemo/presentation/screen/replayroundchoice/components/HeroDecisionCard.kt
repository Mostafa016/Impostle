package com.example.nsddemo.presentation.screen.replayroundchoice.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nsddemo.R
import com.example.nsddemo.presentation.components.modifier.brutalistCard
import com.example.nsddemo.presentation.theme.Dimens

@Composable
fun HeroDecisionCard(modifier: Modifier = Modifier) {
    Column(
        modifier =
            modifier
                .rotate(-2f)
                .fillMaxWidth()
                .brutalistCard(
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    borderColor = MaterialTheme.colorScheme.outline,
                    shadowOffset = Dimens.ShadowLarge,
                    borderWidth = Dimens.BorderThick,
                ).padding(vertical = 48.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.sharp_question_mark_24),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(64.dp),
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "WHAT'S\nNEXT?",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            lineHeight = 48.sp,
        )
    }
}
