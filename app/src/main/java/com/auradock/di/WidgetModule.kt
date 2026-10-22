package com.auradock.di

import com.auradock.widget.BaseWidget
import com.auradock.widget.impl.ClockWidget
import com.auradock.widget.impl.TimerWidget
import com.auradock.widget.impl.WeatherWidget
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/**
 * Hilt multibinding module — registers each widget implementation into a
 * [Set]<[BaseWidget]> that is injected into [com.auradock.widget.WidgetManager].
 *
 * To add a new widget: create the implementation and add a @Binds @IntoSet
 * binding here. WidgetManager picks it up automatically at runtime.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class WidgetModule {

    @Binds
    @IntoSet
    abstract fun bindClockWidget(impl: ClockWidget): BaseWidget

    @Binds
    @IntoSet
    abstract fun bindWeatherWidget(impl: WeatherWidget): BaseWidget

    @Binds
    @IntoSet
    abstract fun bindTimerWidget(impl: TimerWidget): BaseWidget
}
