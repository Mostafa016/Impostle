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

    companion object {
        const val TEST_WORD = "TestWord"
    }
}
