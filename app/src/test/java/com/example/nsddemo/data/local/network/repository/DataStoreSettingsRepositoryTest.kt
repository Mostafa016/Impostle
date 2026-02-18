package com.example.nsddemo.data.local.network.repository

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import app.cash.turbine.test
import com.example.nsddemo.data.local.settings.AppLocaleHelper
import com.example.nsddemo.data.repository.DataStoreSettingsRepository
import com.example.nsddemo.domain.model.AppLocales
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException
import java.util.UUID

@ExperimentalCoroutinesApi
class DataStoreSettingsRepositoryTest {

    @get:Rule
    val tmpFolder: TemporaryFolder = TemporaryFolder.builder().assureDeletion().build()

    private lateinit var repository: DataStoreSettingsRepository
    private lateinit var testDataStore: DataStore<Preferences>

    @MockK
    private lateinit var appLocaleHelper: AppLocaleHelper

    private val testDispatcher = StandardTestDispatcher()

    // Unconfined is recommended for DataStore tests to prevent deadlocks on file locks
    private val dataStoreScope = TestScope(UnconfinedTestDispatcher(testDispatcher.scheduler))

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)

        // Mock Log to prevent RuntimeExceptions in tests (as Log is static Android)
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0

        // Create a unique file for every test run to avoid Windows file locking collisions
        val testFile =
            File(tmpFolder.newFolder(), "test_settings_${UUID.randomUUID()}.preferences_pb")

        testDataStore = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { testFile }
        )

        // Default mock behavior
        every { appLocaleHelper.getCurrentLocale() } returns AppLocales.English

        repository = DataStoreSettingsRepository(testDataStore, appLocaleHelper)
    }

    @After
    fun tearDown() {
        // Cancel the scope to release file handles
        dataStoreScope.cancel()
        Dispatchers.resetMain()
    }

    //region --- Reading Defaults ---

    @Test
    fun `GIVEN empty DataStore WHEN userSettings collected THEN emits defaults`() =
        runTest(testDispatcher) {
            repository.userSettings.test {
                val settings = awaitItem()

                assertNull("Player Name should be null by default", settings.playerName)
                assertFalse("Default theme should be false (Light)", settings.isDarkTheme)
                assertEquals("en", settings.languageCode)
                assertTrue("Player ID should be generated", settings.playerId.isNotEmpty())

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `GIVEN System is Arabic WHEN userSettings collected (empty DS) THEN emits Arabic code`() =
        runTest(testDispatcher) {
            every { appLocaleHelper.getCurrentLocale() } returns AppLocales.Arabic

            repository.userSettings.test {
                val settings = awaitItem()
                assertEquals("ar", settings.languageCode)
                cancelAndIgnoreRemainingEvents()
            }
        }

    //endregion

    //region --- Writing Updates ---

    @Test
    fun `WHEN setPlayerName called THEN userSettings emits new name`() = runTest(testDispatcher) {
        repository.userSettings.test {
            // 1. Initial State
            assertNull(awaitItem().playerName)

            // 2. Action
            repository.setPlayerName("SuperPlayer")

            // 3. Result
            assertEquals("SuperPlayer", awaitItem().playerName)
        }
    }

    @Test
    fun `WHEN setDarkTheme called THEN userSettings emits new theme`() = runTest(testDispatcher) {
        repository.userSettings.test {
            assertFalse(awaitItem().isDarkTheme)

            repository.setDarkTheme(true)

            assertTrue(awaitItem().isDarkTheme)
        }
    }

    @Test
    fun `WHEN setLanguage called THEN updates DataStore AND calls Helper`() =
        runTest(testDispatcher) {
            every { appLocaleHelper.changeLocale(any()) } returns Unit

            repository.userSettings.test {
                assertEquals("en", awaitItem().languageCode)

                repository.setLanguage(AppLocales.Arabic)

                assertEquals("ar", awaitItem().languageCode)
            }

            verify(exactly = 1) { appLocaleHelper.changeLocale(AppLocales.Arabic) }
        }

    //endregion

    //region --- Edge Cases ---

    @Test
    fun `GIVEN DataStore throws IOException WHEN collecting THEN emits empty preferences`() =
        runTest(testDispatcher) {
            // Mock a broken DataStore
            val brokenDataStore = mockk<DataStore<Preferences>>()
            every { brokenDataStore.data } returns flow {
                throw IOException("File corrupted")
            }

            val brokenRepo = DataStoreSettingsRepository(brokenDataStore, appLocaleHelper)

            brokenRepo.userSettings.test {
                val settings = awaitItem()
                // Should fallback to default (empty prefs)
                assertNull(settings.playerName)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `GIVEN DataStore throws Other Exception WHEN collecting THEN crashes`() =
        runTest(testDispatcher) {
            val brokenDataStore = mockk<DataStore<Preferences>>()
            every { brokenDataStore.data } returns flow {
                throw RuntimeException("Something unexpected")
            }

            val brokenRepo = DataStoreSettingsRepository(brokenDataStore, appLocaleHelper)

            brokenRepo.userSettings.test {
                // We expect a crash here, so we await the Error
                val error = awaitError()

                assertTrue(error is RuntimeException)
                assertEquals("Something unexpected", error.message)
            }
        }

    //endregion
}