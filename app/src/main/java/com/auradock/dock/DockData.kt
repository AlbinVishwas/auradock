package com.auradock.dock

import android.content.Context
import com.auradock.data.CalendarEvent
import com.auradock.data.CalendarRepository
import com.auradock.data.NotificationItem
import com.auradock.data.NotificationRepository
import com.auradock.permissions.isNotificationAccessEnabled
import com.auradock.time.MinuteClock
import com.auradock.weather.Weather
import com.auradock.weather.WeatherRepository
import com.auradock.ui.viewmodel.CalendarPermissionUiState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Everything the dock screen shows, as plain singletons.
 *
 * The screensaver ([DockDreamService]) cannot use [com.auradock.ui.viewmodel.StandbyViewModel]:
 * ViewModels are tied to an Activity and a screensaver has none. Anything injected from here lives for
 * the whole app process, so both the Activity and the screensaver can read the same data.
 */
@Singleton
class DockData @Inject constructor(
    @ApplicationContext private val context: Context,
    private val calendarRepository: CalendarRepository,
    notificationRepository: NotificationRepository,
    private val minuteClock: MinuteClock,
    templateRepository: TemplateRepository,
    private val weatherRepository: WeatherRepository
) {
    val selectedTemplate: StateFlow<DockTemplate> = templateRepository.selected

    /** The latest weather (may be old: use usableAt before showing it). */
    val weather: StateFlow<Weather?> = weatherRepository.weather

    /** Call when the dock or app becomes visible: refreshes only if the cache is 30+ minutes old. */
    fun refreshWeather() = weatherRepository.refreshIfStale()

    /** Ticks at the start of every minute (see [MinuteClock]). */
    val timeMillis: Flow<Long> get() = minuteClock.minutes
    fun currentTimeMillis(): Long = minuteClock.currentMillis()

    val todayEvents: Flow<List<CalendarEvent>> get() = calendarRepository.observeTodayEvents()
    val notifications: StateFlow<List<NotificationItem>> = notificationRepository.importantNotifications

    /** Only GRANTED/DENIED here: asking again (and "permanently denied") needs an Activity. */
    fun calendarPermissionState(): CalendarPermissionUiState =
        if (calendarRepository.hasCalendarPermission()) CalendarPermissionUiState.GRANTED
        else CalendarPermissionUiState.DENIED

    fun hasNotificationAccess(): Boolean = isNotificationAccessEnabled(context)
}
