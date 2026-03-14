package com.example.nsddemo.domain.e2e.fakes

import com.example.nsddemo.domain.model.GameCategory
import com.example.nsddemo.domain.repository.WordRepository

/**
 * A deterministic [WordRepository] for E2E tests.
 *
 * Returns a single-element list for every category, so `getWordsForCategory(cat).random()`
 * always returns the same word. This ensures the assigned word is predictable in test assertions.
 */
class FakeWordRepository : WordRepository {
    override fun getWordsForCategory(category: GameCategory): List<String> = listOf(TEST_WORD)
    override fun getSemanticWords(word: String): List<String> = listOf(
        TEST_WORD_SEMANTIC_1,
        TEST_WORD_SEMANTIC_2,
        TEST_WORD_SEMANTIC_3,
        TEST_WORD_RANDOM_FROM_CATEGORY_1,
        TEST_WORD_RANDOM_FROM_CATEGORY_2
    )

    companion object {
        const val TEST_WORD = "TestWord"
        const val TEST_WORD_SEMANTIC_1 = "SemanticWord1"
        const val TEST_WORD_SEMANTIC_2 = "SemanticWord2"
        const val TEST_WORD_SEMANTIC_3 = "SemanticWord3"
        const val TEST_WORD_RANDOM_FROM_CATEGORY_1 = "CategoryRandom1"
        const val TEST_WORD_RANDOM_FROM_CATEGORY_2 = "CategoryRandom2"
    }
}
