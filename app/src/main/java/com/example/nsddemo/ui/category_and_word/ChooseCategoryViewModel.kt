package com.example.nsddemo.ui.category_and_word

import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.nsddemo.Categories
import com.example.nsddemo.Debugging.TAG
import com.example.nsddemo.ui.GameViewModel

class ChooseCategoryViewModel(private val gameViewModel: GameViewModel) : ViewModel() {
    private var _chosenCategory: MutableState<Categories?> = mutableStateOf(null)
    val chosenCategory: State<Categories?> = _chosenCategory
    fun chooseCategory(category: Categories) {
        Log.d(TAG, "chooseCategory: $category")
        _chosenCategory.value = category
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