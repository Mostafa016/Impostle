package com.example.nsddemo.ui.category_and_word

import android.content.Context
import android.net.nsd.NsdManager
import android.net.wifi.WifiManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nsddemo.Categories
import com.example.nsddemo.GameState
import com.example.nsddemo.ui.GameViewModel

class ChooseCategoryViewModel(private val gameViewModel: GameViewModel) : ViewModel() {
    private lateinit var chosenCategory: Categories
    fun chooseCategory(category: Categories) {
        chosenCategory = category
        gameViewModel.updateGameState(
            GameState.DisplayCategoryAndWord(
                chosenCategory.toString(), chosenCategory.words.random()
            )
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