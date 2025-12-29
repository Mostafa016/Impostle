package com.example.nsddemo.data.local.network.repository

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

    // We use a separate scope for DataStore internals to control its lifecycle
    private val dataStoreScope = TestScope(UnconfinedTestDispatcher(testDispatcher.scheduler))

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)

        // FIX 1: Use UnconfinedTestDispatcher for DataStore.
        // This forces DataStore coroutines to run immediately on the current thread,
        // reducing the chance of background threads holding file locks during the Rename operation.

        // FIX 2: Create a unique subfolder for every test.
        // Windows locking can sometimes affect the parent directory.
        val testFile =
            File(tmpFolder.newFolder(), "test_settings_${UUID.randomUUID()}.preferences_pb")

        testDataStore = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { testFile }
        )

        every { appLocaleHelper.getCurrentLocale() } returns AppLocales.English

        repository = DataStoreSettingsRepository(testDataStore, appLocaleHelper)
    }

    @After
    fun tearDown() {
        // FIX 3: Aggressively cancel the DataStore scope.
        // This releases any file handles held by active coroutines.
        dataStoreScope.cancel()
        Dispatchers.resetMain()
    }

    //region --- Reading Defaults ---

    @Test
    fun `GIVEN empty DataStore WHEN userSettings collected THEN emits defaults`() =
        runTest(testDispatcher) {
            every { appLocaleHelper.getCurrentLocale() } returns AppLocales.English

            repository.userSettings.test {
                val settings = awaitItem()

                assertEquals("DEFAULT_PLAYER_NAME", settings.playerName)
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
            assertEquals("DEFAULT_PLAYER_NAME", awaitItem().playerName)

            repository.setPlayerName("SuperPlayer")

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
            val brokenDataStore = mockk<DataStore<Preferences>>()
            every { brokenDataStore.data } returns flow {
                throw IOException("File corrupted")
            }

            val brokenRepo = DataStoreSettingsRepository(brokenDataStore, appLocaleHelper)

            brokenRepo.userSettings.test {
                val settings = awaitItem()
                assertEquals("DEFAULT_PLAYER_NAME", settings.playerName)
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
                // Fix: Use awaitError() to consume the terminal exception event
                val error = awaitError()

                assertTrue(error is RuntimeException)
                assertEquals("Something unexpected", error.message)
            }
        }

    //endregion
}