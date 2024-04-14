package com.example.nsddemo.ui.join_game

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp


@Composable
fun GameCodeTextField(
    modifier: Modifier = Modifier,
    codeLength: Int,
    value: String,
    onValueChange: (String) -> Unit,
    textStyle: TextStyle,
    enabled: Boolean,
    onDonePressed: () -> Unit
) {
    val codeTextFieldValue = remember(value) {
        mutableStateOf(
            TextFieldValue(
                value, TextRange(value.length)
            )
        )
    }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        BasicTextField(
            value = codeTextFieldValue.value,
            onValueChange = { onValueChange(it.text) },
            modifier = modifier.focusRequester(focusRequester = focusRequester),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = { onDonePressed() }
            ),
            decorationBox = {
                GameCodeTextFieldDecoration(
                    code = codeTextFieldValue.value.text,
                    length = codeLength,
                    textStyle = textStyle
                )
            },
            enabled = enabled,
        )
    }
}

@Composable
private fun GameCodeTextFieldDecoration(code: String, length: Int, textStyle: TextStyle) {
    Box(modifier = Modifier) {
        Row(horizontalArrangement = Arrangement.Absolute.Center) {
            for (i in 0 until length) {
                val text = if (i < code.length) code[i].toString() else ""
                CodeEntry(text = text, textStyle = textStyle)
            }
        }
    }
}

@Composable
private fun CodeEntry(text: String, textStyle: TextStyle) {
    Box(
        modifier = Modifier
            .padding(4.dp)
            .width(35.dp)
            .height(55.dp),
        contentAlignment = Alignment.Center
    ) {
        val color = animateColorAsState(
            targetValue = if (text.isEmpty()) Color.Gray.copy(alpha = .8f)
            else MaterialTheme.colorScheme.primary.copy(alpha = .8f)
        )
        Text(
            modifier = Modifier.align(Alignment.Center),
            text = text,
            style = textStyle,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 6.dp, end = 6.dp, bottom = 8.dp)
                .height(2.dp)
                .fillMaxWidth()
                .background(color.value)
        )
    }
}