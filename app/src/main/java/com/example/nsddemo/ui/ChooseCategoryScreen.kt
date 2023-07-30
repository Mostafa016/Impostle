package com.example.nsddemo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nsddemo.R

//TODO: Find a more generalizable solution to show all categories
@Composable
fun ChooseCategoryScreen(viewModel: TestViewModel, onCategoryCardClick: (Categories) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CategoryCard(Categories.Animals, onClick = onCategoryCardClick)
        Spacer(modifier = Modifier.height(32.dp))
        CategoryCard(Categories.Food, onClick = onCategoryCardClick)
        Spacer(modifier = Modifier.height(32.dp))
        CategoryCard(Categories.Jobs, onClick = onCategoryCardClick)
    }
}

@Composable
fun CategoryCard(category: Categories, onClick: (Categories) -> Unit) {
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
            .clickable { onClick(category) },

        ) {
        Text(
            category.name,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
            style = TextStyle(
                fontSize = 24.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
        )
    }
}