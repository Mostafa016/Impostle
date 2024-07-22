package com.example.nsddemo.presentation.screen.main_menu.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.nsddemo.R
import com.example.nsddemo.presentation.theme.englishTypography

@Composable
fun ChangePlayerNameButton(
    modifier: Modifier = Modifier,
    playerName: String?,
    onClick: () -> Unit
) {
    Row(
        modifier
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Icon(
            modifier = Modifier.size(32.dp),
            imageVector = Icons.Filled.Person,
            contentDescription = stringResource(R.string.player_name_icon),
            tint = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            playerName
                ?: stringResource(R.string.player_name_placeholder),
            style = englishTypography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}