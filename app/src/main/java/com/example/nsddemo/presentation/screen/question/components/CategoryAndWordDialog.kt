package com.example.nsddemo.presentation.screen.question.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.nsddemo.R
import com.example.nsddemo.presentation.components.common.BrutalistButton
import com.example.nsddemo.presentation.components.modifier.brutalistCard
import com.example.nsddemo.presentation.theme.BrutalistDimens

@Composable
fun CategoryAndWordDialog(
    category: String,
    word: String?,
    isImposter: Boolean,
    onDismissRequest: () -> Unit,
    onOkClick: () -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .brutalistCard(
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    borderColor = MaterialTheme.colorScheme.outline,
                    shadowOffset = BrutalistDimens.ShadowLarge,
                    borderWidth = BrutalistDimens.BorderThick
                )
        ) {
            // Blue Top Tab
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .brutalistCard(
                        backgroundColor = MaterialTheme.colorScheme.secondary,
                        borderColor = MaterialTheme.colorScheme.outline,
                        shadowOffset = 0.dp,
                        cornerRadius = 8.dp,
                        borderWidth = 2.dp
                    )
                    .padding(horizontal = 24.dp, vertical = 6.dp)
                    .offset(y = (-2).dp) // Overlap top border slightly
            ) {
                Text(
                    text = "SECRET WORD",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondary
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // The Inner Card (Orange for Imposter, White for Innocent)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .brutalistCard(
                            backgroundColor = if (isImposter) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            borderColor = MaterialTheme.colorScheme.outline,
                            shadowOffset = BrutalistDimens.ShadowMedium,
                            borderWidth = BrutalistDimens.BorderThick
                        )
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isImposter) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        MaterialTheme.colorScheme.surface,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        2.dp,
                                        MaterialTheme.colorScheme.outline,
                                        RoundedCornerShape(8.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "?",
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.you_are_the_imposter).uppercase(),
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onPrimary,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "CATEGORY:\n$category".uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = word?.uppercase() ?: "",
                                style = MaterialTheme.typography.displayMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Memorize your role! Don't let others see your screen.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                BrutalistButton(
                    text = "DONE",
                    containerColor = MaterialTheme.colorScheme.outline, // Black button
                    contentColor = MaterialTheme.colorScheme.surface, // White text
                    onClick = onOkClick
                )
            }
        }
    }
}