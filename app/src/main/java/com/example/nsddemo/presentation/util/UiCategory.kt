package com.example.nsddemo.presentation.util

import androidx.compose.ui.graphics.Color
import com.example.nsddemo.R
import com.example.nsddemo.domain.model.GameCategory

enum class UiCategory(
    val domainCategory: GameCategory,
    val nameResId: Int,
    val iconResId: Int,
    val color: Color
) {
    Animals(GameCategory.ANIMALS, R.string.animals, R.drawable.sharp_pets_24, Color.Green),
    Food(GameCategory.FOOD, R.string.food, R.drawable.sharp_fastfood_24, Color.Red),
    Jobs(GameCategory.JOBS, R.string.jobs, R.drawable.sharp_work_24, Color.Blue);
}

val GameCategory.uiCategory: UiCategory
    get() = UiCategory.entries.find { it.domainCategory == this }!!
