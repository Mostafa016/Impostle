package com.example.nsddemo.di

import com.example.nsddemo.domain.model.GameMode
import com.example.nsddemo.domain.strategy.GameModeStrategy
import com.example.nsddemo.domain.strategy.QuestionGameModeStrategy
import dagger.Binds
import dagger.MapKey
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap

@Module
@InstallIn(SingletonComponent::class)
abstract class StrategyModule {
    @Binds
    @IntoMap
    @GameModeKey(GameMode.Question)
    abstract fun bindQuestionStrategy(strategy: QuestionGameModeStrategy): GameModeStrategy

    // Future:
    // @Binds @IntoMap @GameModeKey(GameMode.Describe)
    // abstract fun bindDescribeStrategy(impl: DescribeGameModeStrategy): GameModeStrategy
}

// The Map Key Definition
@MapKey
annotation class GameModeKey(
    val value: GameMode,
)
