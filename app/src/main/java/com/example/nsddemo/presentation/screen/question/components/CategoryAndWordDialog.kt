package com.example.nsddemo.presentation.screen.question.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.nsddemo.R

@Composable
fun CategoryAndWordDialog(
    category: String,
    word: String?,
    isImposter: Boolean,
    onDismissRequest: () -> Unit,
    onOkClick: () -> Unit,
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Column(
            Modifier.background(MaterialTheme.colorScheme.background),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                stringResource(R.string.category, category),
                modifier = Modifier.padding(4.dp),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = if (isImposter) stringResource(R.string.you_are_the_imposter)
                else stringResource(id = R.string.word, word!!),
                modifier = Modifier.padding(4.dp),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .width(1.dp)
                    .padding(horizontal = 4.dp)
            )
            TextButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                onClick = onOkClick,
            ) {
                Text(stringResource(R.string.ok), style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}