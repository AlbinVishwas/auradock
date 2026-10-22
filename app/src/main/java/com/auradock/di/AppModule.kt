package com.auradock.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Application-scoped [CoroutineScope] with a [SupervisorJob].
     * Using [Dispatchers.Default] so widget data collection doesn't block the main thread.
     * Injected into [com.auradock.widget.WidgetManager] and [com.auradock.core.StandbyController].
     */
    @Singleton
    @Provides
    fun provideApplicationScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
