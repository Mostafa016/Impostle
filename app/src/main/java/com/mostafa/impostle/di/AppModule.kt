package com.mostafa.impostle.di

import android.content.Context
import android.net.nsd.NsdManager
import androidx.appcompat.app.AppCompatActivity
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.mostafa.impostle.data.local.network.WifiHelper
import com.mostafa.impostle.data.local.settings.AppLocaleHelper
import com.mostafa.impostle.presentation.navigation.GameRouteMapper
import com.mostafa.impostle.presentation.service.AndroidSessionController
import com.mostafa.impostle.presentation.service.SessionController
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "impostle_game_settings",
)

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideNsdManager(
        @ApplicationContext context: Context,
    ): NsdManager = context.getSystemService(AppCompatActivity.NSD_SERVICE) as NsdManager

    @Provides
    @Singleton
    fun provideWifiHelper(
        @ApplicationContext context: Context,
    ): WifiHelper = WifiHelper(context)

    @Provides
    @Singleton
    fun provideSettingsPreferencesDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = context.settingsDataStore

    @Provides
    @Singleton
    fun provideAppLocaleHelper(): AppLocaleHelper = AppLocaleHelper()

    @Provides
    @Singleton
    fun provideGameRouteMapper(): GameRouteMapper = GameRouteMapper()

    @Module
    @InstallIn(SingletonComponent::class)
    abstract class ControllerModule {
        @Binds
        @Singleton
        abstract fun bindSessionController(impl: AndroidSessionController): SessionController
    }
}
