package com.example.nsddemo.presentation.screen.join_game.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nsddemo.R
import com.example.nsddemo.core.util.GameConstants
import com.example.nsddemo.presentation.components.common.BrutalistSectionHeader
import com.example.nsddemo.presentation.components.modifier.brutalistCard
import com.example.nsddemo.presentation.theme.BrutalistDimens

@Composable
fun JoinGameInputCard(
    codeText: String,
    onCodeChange: (String) -> Unit,
    onJoinClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .brutalistCard(
                backgroundColor = MaterialTheme.colorScheme.surface,
                borderColor = MaterialTheme.colorScheme.outline,
                shadowOffset = BrutalistDimens.ShadowMedium,
                borderWidth = BrutalistDimens.BorderThick
            )
    ) {
        BrutalistSectionHeader(
            text = stringResource(R.string.enter_game_code),
            backgroundColor = Color.Transparent
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .brutalistCard(
                        backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        borderColor = MaterialTheme.colorScheme.outline,
                        cornerRadius = BrutalistDimens.CornerMedium,
                        shadowOffset = 0.dp,
                        borderWidth = 2.dp
                    ),
                contentAlignment = Alignment.Center
            ) {
                GameCodeTextField(
                    codeLength = GameConstants.CODE_LENGTH,
                    value = codeText,
                    onValueChange = onCodeChange,
                    textStyle = MaterialTheme.typography.displayMedium.copy(
                        letterSpacing = 8.sp,
                        textAlign = TextAlign.Center
                    ),
                    enabled = true,
                    onDonePressed = onJoinClick
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "ASK THE HOST FOR THE 4-DIGIT CODE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}
