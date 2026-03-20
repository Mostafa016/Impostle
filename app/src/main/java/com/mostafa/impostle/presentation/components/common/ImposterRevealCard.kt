package com.mostafa.impostle.presentation.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import com.mostafa.impostle.R
import com.mostafa.impostle.presentation.components.modifier.brutalistCard
import com.mostafa.impostle.presentation.theme.Dimens

/**
 * A unified "Hero" card for highlighting the Imposter reveal.
 */
@Composable
fun ImposterRevealCard(
    modifier: Modifier = Modifier,
    imposterName: String,
    imposterColor: Color,
    isCurrentUserImposter: Boolean,
    isWasLabelVisible: Boolean = true,
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
                ).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = if (isCurrentUserImposter) stringResource(R.string.you) else imposterName.uppercase(),
            style = MaterialTheme.typography.displayMedium,
            color = imposterColor,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        if (isWasLabelVisible) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isCurrentUserImposter) stringResource(R.string.were_the_imposter) else stringResource(R.string.was_the_imposter),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )

            // Underline decoration
            Box(
                modifier =
                    Modifier
                        .padding(top = 16.dp)
                        .height(4.dp)
                        .width(64.dp)
                        .background(MaterialTheme.colorScheme.outline, RoundedCornerShape(2.dp)),
            )
        }
    }
}
