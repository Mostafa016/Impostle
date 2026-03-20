package com.mostafa.impostle.domain.repository

import com.mostafa.impostle.domain.model.GameCategory

interface WordRepository {
    fun getWordsForCategory(category: GameCategory): List<String>

    fun getSemanticWords(word: String): List<String>
}
