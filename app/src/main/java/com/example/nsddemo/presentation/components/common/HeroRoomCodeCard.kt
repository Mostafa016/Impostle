package com.example.nsddemo.presentation.components.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nsddemo.presentation.components.modifier.brutalistCard
import com.example.nsddemo.presentation.theme.Dimens

@Composable
fun HeroRoomCodeCard(
    code: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .brutalistCard(
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    borderColor = MaterialTheme.colorScheme.outline,
                    shadowOffset = Dimens.ShadowMedium,
                    borderWidth = Dimens.BorderThin,
                ),
    ) {
        BrutalistSectionHeader(
            text = "ROOM CODE",
            backgroundColor = Color.Transparent,
        )

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp, horizontal = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = code,
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onSurface,
                letterSpacing = 4.sp,
            )
        }
    }
}
