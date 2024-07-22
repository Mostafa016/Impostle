package com.example.nsddemo.presentation.util

import androidx.compose.runtime.Composable

@Composable
fun ConditionalComposable(
    condition: Boolean,
    fallbackComposable: @Composable () -> Unit = {},
    composable: @Composable () -> Unit,
) {
    if (condition) {
        composable()
    } else {
        fallbackComposable()
    }
}