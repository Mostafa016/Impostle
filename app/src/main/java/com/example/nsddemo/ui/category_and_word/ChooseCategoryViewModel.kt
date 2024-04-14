package com.example.nsddemo.ui.category_and_word

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.nsddemo.Categories
import com.example.nsddemo.Debugging.TAG
import com.example.nsddemo.ui.GameViewModel

class ChooseCategoryViewModel(private val gameViewModel: GameViewModel) : ViewModel() {
    fun chooseCategory(category: Categories) {
        Log.d(TAG, "Chosen category: $category")
        gameViewModel.gameRepository.updateGameData(
            gameViewModel.gameRepository.gameData.value.copy(categoryOrdinal = category.ordinal)
        )
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        class ChooseCategoryViewModelFactory(private val gameViewModel: GameViewModel) :
            ViewModelProvider.NewInstanceFactory() {
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ChooseCategoryViewModel(gameViewModel) as T
        }
    }
}