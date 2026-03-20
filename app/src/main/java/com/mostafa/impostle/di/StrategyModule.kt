package com.mostafa.impostle.di

import com.mostafa.impostle.domain.model.GameMode
import com.mostafa.impostle.domain.strategy.GameModeStrategy
import com.mostafa.impostle.domain.strategy.QuestionGameModeStrategy
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
