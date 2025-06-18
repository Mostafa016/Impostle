package com.example.nsddemo.di

import android.content.Context
import android.content.SharedPreferences
import android.net.nsd.NsdManager
import androidx.appcompat.app.AppCompatActivity
import com.example.nsddemo.data.local.network.WifiHelper
import com.example.nsddemo.data.repository.GameRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences("impostle_game_prefs", Context.MODE_PRIVATE)
    }

    @Provides
    @Singleton
    fun provideGameRepository(sharedPreferences: SharedPreferences): GameRepository {
        return GameRepository(sharedPreferences)
    }

    @Provides
    @Singleton
    fun provideNsdManager(@ApplicationContext context: Context): NsdManager {
        return context.getSystemService(AppCompatActivity.NSD_SERVICE) as NsdManager
    }

    @Provides
    @Singleton
    fun provideWifiHelper(@ApplicationContext context: Context): WifiHelper {
        return WifiHelper(context)
    }
}