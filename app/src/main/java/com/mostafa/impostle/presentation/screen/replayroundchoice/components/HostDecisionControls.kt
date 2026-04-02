package com.mostafa.impostle.presentation.screen.replayroundchoice.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.mostafa.impostle.R
import com.mostafa.impostle.presentation.components.common.BrutalistButton
import com.mostafa.impostle.presentation.theme.Dimens

@Composable
fun HostDecisionControls(
    isReplayEnabled: Boolean,
    isVoteEnabled: Boolean,
    onReplayClick: () -> Unit,
    onVoteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium),
    ) {
        BrutalistButton(
            text = stringResource(R.string.additional_round),
            subtext = stringResource(R.string.ask_more_questions),
            icon = ImageVector.vectorResource(R.drawable.sharp_add_24),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            enabled = isReplayEnabled,
            onClick = onReplayClick,
        )

        BrutalistButton(
            text = stringResource(R.string.start_vote),
            subtext = stringResource(R.string.find_the_impostor),
            icon = ImageVector.vectorResource(R.drawable.sharp_target_24),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            enabled = isVoteEnabled,
            onClick = onVoteClick,
        )
    }
}
