package com.example.nsddemo.presentation.screen.settings

import app.cash.turbine.test
import com.example.nsddemo.presentation.fakes.FakeSettingsRepository
import com.example.nsddemo.presentation.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeSettingsRepo: FakeSettingsRepository
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        fakeSettingsRepo = FakeSettingsRepository()
        viewModel = SettingsViewModel(fakeSettingsRepo)
    }

    @Test
    fun `WHEN theme changed THEN updates repository and state flow`() = runTest {
        viewModel.darkThemeSetting.test {
            // Initial (default from fake is false)
            assertFalse(awaitItem())

            viewModel.onThemeChange(true)
            advanceUntilIdle()

            assertTrue(awaitItem())
        }
    }

    @Test
    fun `WHEN language changed THEN updates repository and closes dropdown`() = runTest {
        viewModel.languageSetting.test {
            // Initial is English
            assertEquals(GameLocales.English, awaitItem())

            // Act
            viewModel.onLanguageChange(GameLocales.Arabic)
            advanceUntilIdle()

            // Assert
            assertEquals(GameLocales.Arabic, awaitItem())
            assertFalse(viewModel.languageSettingDropdownExpanded.value)
        }
    }

    @Test
    fun `WHEN dropdown events triggered THEN updates dropdown state`() {
        viewModel.onLanguageDropDownExpandedChange(true)
        assertTrue(viewModel.languageSettingDropdownExpanded.value)

        viewModel.onLanguageDropDownDismiss()
        assertFalse(viewModel.languageSettingDropdownExpanded.value)
    }
}