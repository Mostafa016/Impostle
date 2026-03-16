package com.example.nsddemo.domain.repository

import com.example.nsddemo.domain.model.GameCategory

interface WordRepository {
    fun getWordsForCategory(category: GameCategory): List<String>

    fun getSemanticWords(word: String): List<String>
}
