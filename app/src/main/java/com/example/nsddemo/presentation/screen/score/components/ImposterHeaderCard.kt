package com.example.nsddemo.presentation.screen.score.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nsddemo.R
import com.example.nsddemo.presentation.components.modifier.brutalistCard
import com.example.nsddemo.presentation.theme.Dimens

@Composable
fun ImposterHeaderCard(
    modifier: Modifier = Modifier,
    imposterName: String,
    imposterColor: Color,
    isCurrentUserImposter: Boolean,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .brutalistCard(
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    borderColor = MaterialTheme.colorScheme.outline,
                    shadowOffset = Dimens.ShadowLarge,
                    borderWidth = Dimens.BorderThick,
                ).padding(vertical = 32.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier =
                Modifier
                    .size(80.dp)
                    .background(imposterColor, RoundedCornerShape(12.dp))
                    .border(
                        Dimens.BorderThick,
                        MaterialTheme.colorScheme.outline,
                        RoundedCornerShape(12.dp),
                    ),
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (isCurrentUserImposter) stringResource(R.string.you) else imposterName.uppercase(),
            style = MaterialTheme.typography.displayMedium.copy(fontSize = 32.sp),
            color = imposterColor,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = if (isCurrentUserImposter) stringResource(R.string.were_the_imposter) else stringResource(R.string.was_the_imposter),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            lineHeight = 32.sp,
        )
    }
}
