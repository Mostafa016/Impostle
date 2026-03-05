package com.example.nsddemo.presentation.screen.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.nsddemo.presentation.components.modifier.brutalistCard
import com.example.nsddemo.presentation.screen.settings.GameLocales
import com.example.nsddemo.presentation.theme.BrutalistDimens

@Composable
fun BrutalistDropdown(
    currentValue: String,
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    onDismissRequest: () -> Unit,
    options: List<GameLocales>,
    onOptionSelected: (GameLocales) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .brutalistCard(
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    borderColor = MaterialTheme.colorScheme.outline,
                    cornerRadius = BrutalistDimens.CornerMedium,
                    shadowOffset = BrutalistDimens.ShadowSmall,
                    borderWidth = BrutalistDimens.BorderThin
                )
                .clickable { onExpandChange(!expanded) }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = currentValue.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissRequest,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    BrutalistDimens.BorderThin,
                    MaterialTheme.colorScheme.outline,
                    RoundedCornerShape(4.dp)
                )
        ) {
            options.forEach { locale ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(locale.languageStringResId).uppercase(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        onOptionSelected(locale)
                        onDismissRequest()
                    }
                )
            }
        }
    }
}
