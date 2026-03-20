package com.mostafa.impostle.presentation.util

import androidx.compose.ui.graphics.Color
import com.mostafa.impostle.domain.model.NewPlayerColors

fun NewPlayerColors.toComposeColor(): Color = Color(this.hexCode.toLong(radix = 16))
