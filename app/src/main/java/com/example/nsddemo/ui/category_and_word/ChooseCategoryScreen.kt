package com.example.nsddemo.ui.category_and_word

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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
import com.example.nsddemo.Categories

@Composable
fun ChooseCategoryScreen(vm: ChooseCategoryViewModel, onNavigateToCategoryAndWord: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Categories.values().forEachIndexed { index, category ->
            CategoryCard(category, onClick = {
                vm.chooseCategory(category)
                onNavigateToCategoryAndWord()
            })
            if (index != Categories.values().lastIndex) Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun CategoryCard(category: Categories, onClick: () -> Unit) {
    Box(
        Modifier
            .requiredSize(300.dp)
            .clip(CircleShape)
            .paint(
                painterResource(category.imageDrawableID), contentScale = ContentScale.Crop
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
            stringResource(category.nameResourceId),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
            style = MaterialTheme.typography.headlineLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold,
        )
    }
}