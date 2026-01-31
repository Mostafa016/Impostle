package com.example.nsddemo.presentation.screen.choose_category.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.nsddemo.presentation.util.UiCategory

@Composable
fun CategoryCard(modifier: Modifier, category: UiCategory, onClick: () -> Unit) {
    Box(
        modifier
            .clip(CircleShape)
            .paint(
                painterResource(category.iconResId), contentScale = ContentScale.Crop
            )
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.Transparent, category.color
                    ),
                )
            )
            .clickable { onClick() },
    ) {
        Text(
            stringResource(category.nameResId),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
            style = MaterialTheme.typography.headlineLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold,
        )
    }
}