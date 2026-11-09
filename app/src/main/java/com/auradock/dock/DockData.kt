package com.auradock.dock

import com.auradock.ui.theme.BoldPalette
import android.content.Context
import com.auradock.data.CalendarEvent
import com.auradock.data.CalendarRepository
import com.auradock.data.NotificationItem
import com.auradock.data.NotificationRepository
import com.auradock.data.latestPerApp
import com.auradock.focus.FocusSettings
import com.auradock.intention.IntentionRepository
import com.auradock.intention.IntentionToday
import com.auradock.permissions.isNotificationAccessEnabled
import com.auradock.tasks.TaskEntity
import com.auradock.tasks.TaskRepository
import com.auradock.sessions.LiveSession
import com.auradock.sessions.SessionTracker
import com.auradock.time.MinuteClock
import com.auradock.weather.Weather
import com.auradock.weather.WeatherRepository
import com.auradock.ui.viewmodel.CalendarPermissionUiState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
    focusSettings: FocusSettings,
    private val minuteClock: MinuteClock,
    templateRepository: TemplateRepository,
    private val weatherRepository: WeatherRepository,
    taskRepository: TaskRepository,
    sessionTracker: SessionTracker,
    private val intentions: IntentionRepository
) {
    /** The focus session going on now, for the dock's ring and its "held for you" line. */
    val session: StateFlow<LiveSession?> = sessionTracker.live

    /** Today's morning intention (null until it has been read). */
    val intention: Flow<IntentionToday> = intentions.today
    fun intentionOffered() { intentions.markOffered() }
    fun saveIntention(text: String) { intentions.save(text) }
    fun skipIntention() { intentions.skip() }
    fun setIntentionDone(done: Boolean) { intentions.setDone(done) }

    val selectedTemplate: StateFlow<DockTemplate> = templateRepository.selected

    /** Home for the World dock, and the extra cities picked. */
    val worldHome = weatherRepository.place
    val worldCities = templateRepository.worldCities

    /** The Bold clock's colourway. */
    val boldPalette: StateFlow<BoldPalette> = templateRepository.boldPalette

    /** The latest weather (may be old: use usableAt before showing it). */
    val weather: StateFlow<Weather?> = weatherRepository.weather

    /** Call when the dock or app becomes visible: refreshes only if the cache is 30+ minutes old. */
    fun refreshWeather() = weatherRepository.refreshIfStale()

    /** Ticks at the start of every minute (see [MinuteClock]). */
    val timeMillis: Flow<Long> get() = minuteClock.minutes
    fun currentTimeMillis(): Long = minuteClock.currentMillis()

    val todayTasks: Flow<List<TaskEntity>> = taskRepository.todayTasks

    val todayEvents: Flow<List<CalendarEvent>> get() = calendarRepository.observeTodayEvents()
    val notifications: StateFlow<List<NotificationItem>> = notificationRepository.items

    /** What the dock strip shows: the latest notification from each picked app. */
    val stripNotifications: Flow<List<NotificationItem>> =
        combine(notificationRepository.items, focusSettings.pickedApps, ::latestPerApp)
    val showMessageText: StateFlow<Boolean> = focusSettings.showMessageText

    /** Only GRANTED/DENIED here: asking again (and "permanently denied") needs an Activity. */
    fun calendarPermissionState(): CalendarPermissionUiState =
        if (calendarRepository.hasCalendarPermission()) CalendarPermissionUiState.GRANTED
        else CalendarPermissionUiState.DENIED

    fun hasNotificationAccess(): Boolean = isNotificationAccessEnabled(context)
}
