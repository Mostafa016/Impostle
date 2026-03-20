package com.mostafa.impostle.presentation.screen.choosecategory

import com.mostafa.impostle.domain.model.GameCategory

sealed interface ChooseCategoryEvent {
    data class CategoryChosen(
        val category: GameCategory,
    ) : ChooseCategoryEvent

    data object ConfirmSelection : ChooseCategoryEvent
}
