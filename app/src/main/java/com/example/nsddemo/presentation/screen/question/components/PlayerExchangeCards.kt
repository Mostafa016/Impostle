package com.example.nsddemo.presentation.screen.question.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.nsddemo.R
import com.example.nsddemo.domain.model.NewPlayerColors
import com.example.nsddemo.domain.model.Player
import com.example.nsddemo.presentation.components.modifier.brutalistCard
import com.example.nsddemo.presentation.theme.Dimens
import com.example.nsddemo.presentation.util.toComposeColor

@Composable
fun PlayerExchangeCards(
    isCurrentPlayerAsking: Boolean,
    isCurrentPlayerAsked: Boolean,
    askingPlayer: Player,
    askedPlayer: Player,
    modifier: Modifier = Modifier,
) {
    val verticalGap = Dimens.SpacingXLarge

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ASKER CARD
            val askerAvatarColor = NewPlayerColors.fromHex(askingPlayer.color).toComposeColor()
            ExchangeCard(
                title = stringResource(R.string.asker),
                playerName = if (isCurrentPlayerAsking) stringResource(R.string.you) else askingPlayer.name.uppercase(),
                avatarColor = askerAvatarColor,
                isActive = isCurrentPlayerAsking,
                isRightAligned = false,
            )

            Spacer(modifier = Modifier.height(verticalGap))

            // THE BRIDGE PILL
            BridgePill(
                isCurrentPlayerAsking = isCurrentPlayerAsking,
                isCurrentPlayerAsked = isCurrentPlayerAsked,
                askingName = askingPlayer.name.uppercase(),
                askedName = askedPlayer.name.uppercase(),
            )

            Spacer(modifier = Modifier.height(verticalGap))

            // ASKED CARD
            val askedAvatarColor = NewPlayerColors.fromHex(askedPlayer.color).toComposeColor()
            ExchangeCard(
                title = stringResource(R.string.the_asked),
                playerName = if (isCurrentPlayerAsked) stringResource(R.string.you) else askedPlayer.name.uppercase(),
                avatarColor = askedAvatarColor,
                isActive = isCurrentPlayerAsked,
                isRightAligned = true,
            )
        }
    }
}

@Composable
private fun ExchangeCard(
    title: String,
    playerName: String,
    avatarColor: Color,
    isActive: Boolean,
    isRightAligned: Boolean,
) {
    val outlineColor = MaterialTheme.colorScheme.outline
    val cardBorderColor = if (isActive) MaterialTheme.colorScheme.primary else outlineColor
    val cardBgTint =
        if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
    val shadowColor = if (isActive) MaterialTheme.colorScheme.primary else Color.Black

    Box(modifier = Modifier.fillMaxWidth()) {
        if (isActive) {
            Box(
                modifier =
                    Modifier
                        .align(if (isRightAligned) Alignment.TopEnd else Alignment.TopStart)
                        .offset(
                            x = if (isRightAligned) 8.dp else (-8).dp,
                            y = (-12).dp,
                        ).rotate(if (isRightAligned) 4f else -4f)
                        .brutalistCard(
                            backgroundColor = MaterialTheme.colorScheme.primary,
                            borderColor = outlineColor,
                            shadowOffset = 2.dp,
                            cornerRadius = 4.dp,
                        ).padding(horizontal = 12.dp, vertical = 4.dp)
                        .zIndex(10f),
            ) {
                Text(
                    text = stringResource(R.string.your_turn),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .brutalistCard(
                        backgroundColor = cardBgTint,
                        borderColor = cardBorderColor,
                        shadowColor = shadowColor,
                        shadowOffset = Dimens.ShadowMedium,
                        cornerRadius = Dimens.CornerXLarge,
                        borderWidth = Dimens.BorderThick,
                    ).padding(24.dp),
            horizontalArrangement = if (isRightAligned) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!isRightAligned) AvatarCircle(avatarColor)

            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                horizontalAlignment = if (isRightAligned) Alignment.End else Alignment.Start,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = playerName,
                    style = MaterialTheme.typography.displayMedium.copy(fontSize = 32.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
            }

            if (isRightAligned) AvatarCircle(avatarColor)
        }
    }
}

@Composable
private fun AvatarCircle(color: Color) {
    Box(
        modifier =
            Modifier
                .size(80.dp)
                .background(color, CircleShape)
                .border(4.dp, MaterialTheme.colorScheme.outline, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(48.dp),
        )
    }
}

@Composable
private fun BridgePill(
    isCurrentPlayerAsking: Boolean,
    isCurrentPlayerAsked: Boolean,
    askingName: String,
    askedName: String,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(1.dp),
                        ),
            )
            Box(
                modifier =
                    Modifier
                        .brutalistCard(
                            backgroundColor = MaterialTheme.colorScheme.surface,
                            borderColor = MaterialTheme.colorScheme.outline,
                            cornerRadius = Dimens.CornerPill,
                            shadowOffset = Dimens.ShadowSmall,
                            borderWidth = Dimens.BorderThin,
                        ).padding(horizontal = 24.dp, vertical = 10.dp),
            ) {
                val annotatedString =
                    when {
                        isCurrentPlayerAsking ->
                            buildAnnotatedString {
                                append(stringResource(R.string.ask))
                                withStyle(
                                    SpanStyle(
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                ) {
                                    append(askedName)
                                }
                                append(stringResource(R.string.a_question))
                            }

                        isCurrentPlayerAsked ->
                            buildAnnotatedString {
                                withStyle(
                                    SpanStyle(
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                ) {
                                    append(askingName)
                                }
                                append(stringResource(R.string.is_asking_you))
                            }

                        else ->
                            buildAnnotatedString {
                                withStyle(
                                    SpanStyle(
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                ) {
                                    append(askingName)
                                }
                                append(stringResource(R.string.asks))
                                withStyle(
                                    SpanStyle(
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                ) {
                                    append(askedName)
                                }
                            }
                    }

                Text(
                    text = annotatedString.text.uppercase(),
                    style =
                        MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.sp,
                            fontSize = 11.sp,
                        ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            modifier = Modifier.size(24.dp),
        )
    }
}
