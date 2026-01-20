package com.example.nsddemo.presentation.util

import androidx.compose.ui.graphics.Color
import com.example.nsddemo.domain.model.NewPlayerColors

fun NewPlayerColors.toComposeColor(): Color = Color(this.hexCode.toLong(radix = 16))