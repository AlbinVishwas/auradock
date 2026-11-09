package com.auradock.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auradock.data.CalendarEvent
import com.auradock.dock.DockModeState
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import com.auradock.focus.AppCatalog
import com.auradock.focus.AppEntry
import com.auradock.focus.DndController
import com.auradock.focus.DockFocusController
import com.auradock.focus.vip.Vip
import com.auradock.focus.vip.VipContacts
import com.auradock.focus.FocusSettings
import com.auradock.onboarding.OnboardingState
import com.auradock.permissions.canScheduleExactAlarms
import com.auradock.dock.DockTemplate
import com.auradock.dock.TemplateRepository
import com.auradock.tasks.TaskEntity
import com.auradock.tasks.TaskRepository
import com.auradock.weather.Weather
import com.auradock.weather.WeatherRepository
import com.auradock.data.CalendarRepository
import com.auradock.standby.StandbyState
import com.auradock.standby.StandbyTriggerManager
import com.auradock.permissions.canDrawOverOtherApps
import com.auradock.intention.IntentionRepository
import com.auradock.intention.IntentionLine
import com.auradock.intention.intentionLine
import com.auradock.sessions.FocusStats
import com.auradock.sessions.SessionSummary
import com.auradock.sessions.WeeklyFocus
import com.auradock.sessions.SessionTracker
import com.auradock.time.MinuteClock
import com.auradock.watch.DockWatcherController
import android.content.Context
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StandbyViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val calendarRepository: CalendarRepository,
    private val appCatalog: AppCatalog,
    private val templateRepository: TemplateRepository,
    private val taskRepository: TaskRepository,
    private val weatherRepository: WeatherRepository,
    private val focusSettings: FocusSettings,
    private val dndController: DndController,
    private val dockFocus: DockFocusController,
    private val vipContacts: VipContacts,
    private val dockWatcher: DockWatcherController,
    private val onboardingState: OnboardingState,
    private val sessions: SessionTracker,
    private val intentions: IntentionRepository,
    focusStats: FocusStats,
    minuteClock: MinuteClock
) : ViewModel() {

    /** Focus time over the last seven days. Null until the first read, so the line does not flash "start a session". */
    val weeklyFocus: StateFlow<WeeklyFocus?> = focusStats.weekly
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** True while the card of a finished session may ask "Tomorrow's top task?" (an evening session, not yet asked this evening). */
    val shutdownOffer: StateFlow<Boolean> = sessions.summary.map { it != null && intentions.shutdownEligible(it.endedAt) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** The question was shown: not again this evening. */
    fun shutdownShown(endedAt: Long) = intentions.markShutdownOffered(endedAt)

    /** Tomorrow's top task: a task that appears tomorrow (at [dueAt] if given), and tomorrow's suggested morning intention. */
    fun saveTomorrowTask(title: String, dueAt: Long?) {
        viewModelScope.launch {
            val now = minuteClockNow()
            taskRepository.add(title, dueAt, showFrom = com.auradock.intention.tomorrowStart(now))
            intentions.suggestForTomorrow(title)
        }
        if (dueAt != null) askForNotificationsOnce()
    }
    private fun minuteClockNow() = currentTimeMillis.value

    /** Today's morning intention for the top of the Today card (null when there is none). */
    val intention: StateFlow<IntentionLine?> = intentions.today.map { intentionLine(it.entry) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    fun setIntentionDone(done: Boolean) { intentions.setDone(done) }

    /** The card shown after a focus session of two minutes or more, until it is dismissed. */
    val undockSummary: StateFlow<SessionSummary?> = sessions.summary
    fun dismissUndockSummary() = sessions.dismissSummary()


    /** Current time, refreshed at the start of every minute. Shared by the clock, preview and dashboard date. */
    val currentTimeMillis: StateFlow<Long> = minuteClock.minutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), minuteClock.currentMillis())

    /** The saved template choice (shared with the screensaver). */
    val currentTemplate: StateFlow<DockTemplate> = templateRepository.selected

    // Owned by the ViewModel (not the Activity) so a rotation, which destroys and recreates the
    // Activity, does not stop and restart it. It is stopped in onCleared().
    private val standbyTriggerManager = StandbyTriggerManager(context).also { it.start() }
    val standbyState: StateFlow<StandbyState> = standbyTriggerManager.standbyState

    // Whether the in-app dock is showing (auto or "Dock now"). Lives here so rotation does not lose it.
    private val dockMode = MutableStateFlow(DockModeState())
    val dockVisible: StateFlow<Boolean> = combine(dockMode, standbyState) { mode, auto -> mode.visible(auto) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    init {
        // Housekeeping: finished tasks from before today are hidden already; delete them.
        viewModelScope.launch { taskRepository.purgeOldDone() }
        // Alarms can be lost (a restart, a force-stop); set the pending ones again whenever the app opens.
        viewModelScope.launch { taskRepository.rescheduleAlarms() }
        viewModelScope.launch {
            standbyState.collect { auto -> dockMode.update { it.onAutoChanged(auto) } }
        }
    }

    /** Null until the first load finishes, so the screen never claims "nothing planned" before it knows. */
    val tasks: StateFlow<List<TaskEntity>?> = taskRepository.todayTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Adds a task when [id] is null, otherwise edits that task. */
    fun saveTask(id: Long?, title: String, dueAt: Long?) {
        viewModelScope.launch {
            if (id == null) taskRepository.add(title, dueAt) else taskRepository.edit(id, title, dueAt)
        }
        if (dueAt != null) askForNotificationsOnce()
    }

    /** True if AuraDock may set exact alarms (Android 12+ asks the user in settings). */
    private val _exactAlarmsAllowed = MutableStateFlow(canScheduleExactAlarms(context))
    val exactAlarmsAllowed: StateFlow<Boolean> = _exactAlarmsAllowed.asStateFlow()

    fun refreshExactAlarmAccess() {
        _exactAlarmsAllowed.value = canScheduleExactAlarms(context)
    }

    fun openExactAlarmSettings() {
        _permissionActions.tryEmit(PermissionAction.OpenExactAlarmSettings)
    }

    /**
     * The first time a task gets a time, ask for permission to show notifications (Android 13+ shows the system
     * dialog). Only once: if the user says no, we do not nag; reminders just will not appear.
     */
    private fun askForNotificationsOnce() {
        val prefs = context.getSharedPreferences("auradock_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("asked_post_notifications", false)) {
            prefs.edit().putBoolean("asked_post_notifications", true).apply()
            _permissionActions.tryEmit(PermissionAction.RequestNotificationPermission)
        }
    }

    private val _postNotificationsGranted = MutableStateFlow(notificationsAllowed())
    /** True if AuraDock may show notifications (always true before Android 13, unless switched off in settings). */
    val postNotificationsGranted: StateFlow<Boolean> = _postNotificationsGranted.asStateFlow()
    private var postNotificationsBlocked = false // the system will not ask again: only app settings can allow it

    private fun notificationsAllowed() = androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun refreshPostNotifications() {
        _postNotificationsGranted.value = notificationsAllowed()
    }

    fun onPostNotificationsResult(granted: Boolean, permanentlyDenied: Boolean) {
        postNotificationsBlocked = !granted && permanentlyDenied
        refreshPostNotifications()
    }

    /** The user tapped "Allow" for notifications: the system dialog, or app settings once the dialog will not show. */
    fun requestPostNotifications() {
        val canAsk = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU && !postNotificationsBlocked
        _permissionActions.tryEmit(if (canAsk) PermissionAction.RequestNotificationPermission else PermissionAction.OpenAppSettings)
    }

    fun setTaskDone(task: TaskEntity, done: Boolean) {
        viewModelScope.launch { taskRepository.setDone(task.id, done) }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch { taskRepository.delete(task.id) }
    }

    val weather: StateFlow<Weather?> = weatherRepository.weather

    private val _locationPermissionState = MutableStateFlow(locationState())
    val locationPermissionState: StateFlow<CalendarPermissionUiState> = _locationPermissionState.asStateFlow()

    private fun locationState() =
        if (weatherRepository.hasLocationPermission()) CalendarPermissionUiState.GRANTED else CalendarPermissionUiState.DENIED

    fun refreshLocationPermission() {
        // keep "permanently denied" until the user grants it: only Android settings can undo it
        _locationPermissionState.value =
            if (weatherRepository.hasLocationPermission()) CalendarPermissionUiState.GRANTED
            else if (_locationPermissionState.value == CalendarPermissionUiState.PERMANENTLY_DENIED) CalendarPermissionUiState.PERMANENTLY_DENIED
            else CalendarPermissionUiState.DENIED
    }

    fun onLocationPermissionResult(granted: Boolean, permanentlyDenied: Boolean) {
        _locationPermissionState.value = when {
            granted -> CalendarPermissionUiState.GRANTED
            permanentlyDenied -> CalendarPermissionUiState.PERMANENTLY_DENIED
            else -> CalendarPermissionUiState.DENIED
        }
        if (granted) weatherRepository.refreshIfStale()
    }

    /** The user tapped "Allow location for weather". */
    fun requestLocationPermission() {
        when (_locationPermissionState.value) {
            CalendarPermissionUiState.GRANTED -> Unit
            CalendarPermissionUiState.DENIED -> _permissionActions.tryEmit(PermissionAction.RequestLocationPermission)
            CalendarPermissionUiState.PERMANENTLY_DENIED -> _permissionActions.tryEmit(PermissionAction.OpenAppSettings)
        }
    }

    /** The app is visible: refresh the weather if the cache is 30+ minutes old. */
    fun refreshWeather() = weatherRepository.refreshIfStale()

    // ── Focus (Do Not Disturb while docked) ───────────────────────────────────

    val focusWhileDocked: StateFlow<Boolean> = focusSettings.focusWhileDocked

    private val _dndAccessGranted = MutableStateFlow(dndController.hasAccess())
    val dndAccessGranted: StateFlow<Boolean> = _dndAccessGranted.asStateFlow()

    fun setFocusWhileDocked(enabled: Boolean) {
        focusSettings.setFocusWhileDocked(enabled)
        dockFocus.refresh() // switching it off while docked puts the user's settings back at once
    }

    /** The user may have just granted DND access on the system page. */
    fun refreshDndAccess() {
        _dndAccessGranted.value = dndController.hasAccess()
        dockFocus.refreshRespectingGrace()
    }

    fun openDndAccessSettings() {
        _permissionActions.tryEmit(PermissionAction.OpenDndAccessSettings)
    }

    // ── People who can always reach you ───────────────────────────────────────

    val vips: StateFlow<List<Vip>> = focusSettings.vips

    private val _canWriteContacts = MutableStateFlow(vipContacts.canWrite())

    /** On only if the switch is on *and* the permission to change contacts is still granted. */
    val vipRingThrough: StateFlow<Boolean> = combine(focusSettings.vipRingThrough, _canWriteContacts) { on, can -> on && can }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** How many people AuraDock is still holding a loaned mark for (0 unless something went wrong). */
    val vipsOnLoan: StateFlow<Int> = vipContacts.onLoan

    /** "Add a person": ask for the contacts permission only now, then open the contact picker. */
    fun addVipTapped() {
        _permissionActions.tryEmit(if (vipContacts.canRead()) PermissionAction.PickContact else PermissionAction.RequestContactsRead)
    }

    fun onContactsReadResult(granted: Boolean, permanentlyDenied: Boolean) {
        when {
            granted -> _permissionActions.tryEmit(PermissionAction.PickContact)
            permanentlyDenied -> _permissionActions.tryEmit(PermissionAction.OpenAppSettings)
        }
    }

    fun onContactPicked(contact: android.net.Uri) {
        vipContacts.read(contact)?.let { focusSettings.addVip(it) }
        dockFocus.refresh()
    }

    fun removeVip(vip: Vip) {
        focusSettings.removeVip(vip.lookupKey)
        dockFocus.refresh()
    }

    /** Switching on asks for the permission to change contacts first; the switch turns on once it is granted. */
    fun setVipRingThrough(enabled: Boolean) {
        if (enabled && !vipContacts.canWrite()) {
            _permissionActions.tryEmit(PermissionAction.RequestContactsWrite)
            return
        }
        focusSettings.setVipRingThrough(enabled)
        dockFocus.refresh() // off: anything loaned goes back at once
    }

    fun onContactsWriteResult(granted: Boolean, permanentlyDenied: Boolean) {
        _canWriteContacts.value = vipContacts.canWrite()
        when {
            granted -> setVipRingThrough(true)
            permanentlyDenied -> _permissionActions.tryEmit(PermissionAction.OpenAppSettings)
        }
    }

    /** The user may have just changed the contacts permissions in system settings. */
    fun refreshContactsAccess() {
        _canWriteContacts.value = vipContacts.canWrite()
        dockFocus.refresh()
    }

    /** "Put back": gives every loaned mark back now. */
    fun putVipsBack() = vipContacts.giveBack()

    // ── Dock from anywhere (background watcher) ───────────────────────────────

    val dockFromAnywhere: StateFlow<Boolean> = focusSettings.dockFromAnywhere

    private val _overlayGranted = MutableStateFlow(canDrawOverOtherApps(context))
    val overlayGranted: StateFlow<Boolean> = _overlayGranted.asStateFlow()

    /** Turning it on without "Display over other apps" sends the user to that page; the watcher starts once it is allowed. */
    fun setDockFromAnywhere(enabled: Boolean) {
        focusSettings.setDockFromAnywhere(enabled)
        if (enabled && !_overlayGranted.value) _permissionActions.tryEmit(PermissionAction.OpenOverlaySettings)
        dockWatcher.sync()
    }

    fun openOverlaySettings() {
        _permissionActions.tryEmit(PermissionAction.OpenOverlaySettings)
    }

    /** The user may have just allowed (or taken away) the permission on the system page; also restarts a stopped watcher. */
    fun refreshOverlayAccess() {
        _overlayGranted.value = canDrawOverOtherApps(context)
        dockWatcher.sync()
    }

    // ── Apps whose notifications the dock shows ───────────────────────────────

    val pickedApps: StateFlow<List<String>> = focusSettings.pickedApps

    val showMessageText: StateFlow<Boolean> = focusSettings.showMessageText

    fun setShowMessageText(show: Boolean) = focusSettings.setShowMessageText(show)

    private val _apps = MutableStateFlow<List<AppEntry>?>(null)

    /** The launchable apps, or null until loaded. */
    val apps: StateFlow<List<AppEntry>?> = _apps.asStateFlow()

    fun loadApps() {
        if (_apps.value == null) viewModelScope.launch { _apps.value = appCatalog.launchableApps() }
    }

    fun toggleApp(packageName: String) {
        focusSettings.toggleApp(packageName)
    }

    suspend fun appIcon(packageName: String): androidx.compose.ui.graphics.ImageBitmap? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        appCatalog.icon(packageName)?.toBitmap(96, 96)?.asImageBitmap()
    }

    /** False until the first-launch walkthrough has been finished. */
    val onboardingDone: StateFlow<Boolean> = onboardingState.done

    fun finishOnboarding() = onboardingState.complete()

    fun enterDock() = dockMode.update { it.enterManual() }
    fun dismissDock() = dockMode.update { it.dismiss(standbyState.value) }

    private val _hasCalendarPermission = MutableStateFlow(calendarRepository.hasCalendarPermission())
    val hasCalendarPermission: StateFlow<Boolean> = _hasCalendarPermission.asStateFlow()
    private val _calendarPermissionState = MutableStateFlow(
        if (_hasCalendarPermission.value) CalendarPermissionUiState.GRANTED else CalendarPermissionUiState.DENIED
    )
    val calendarPermissionState: StateFlow<CalendarPermissionUiState> = _calendarPermissionState.asStateFlow()
    private val _hasNotificationAccess = MutableStateFlow(false)
    val hasNotificationAccess: StateFlow<Boolean> = _hasNotificationAccess.asStateFlow()
    private val _permissionActions = MutableSharedFlow<PermissionAction>(extraBufferCapacity = 1)
    val permissionActions: SharedFlow<PermissionAction> = _permissionActions
    /** Null until the first load finishes, so the screen never claims "no events" before it knows. */
    val todayEvents: StateFlow<List<CalendarEvent>?> = calendarRepository.observeTodayEvents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setTemplate(template: DockTemplate) {
        templateRepository.select(template)
    }

    override fun onCleared() {
        standbyTriggerManager.stop()
    }

    fun refreshCalendarPermission() {
        val granted = calendarRepository.hasCalendarPermission()
        _hasCalendarPermission.value = granted
        _calendarPermissionState.value = if (granted) {
            CalendarPermissionUiState.GRANTED
        } else {
            CalendarPermissionUiState.DENIED
        }
    }

    fun onCalendarPermissionResult(granted: Boolean, permanentlyDenied: Boolean) {
        _hasCalendarPermission.value = granted
        _calendarPermissionState.value = when {
            granted -> CalendarPermissionUiState.GRANTED
            permanentlyDenied -> CalendarPermissionUiState.PERMANENTLY_DENIED
            else -> CalendarPermissionUiState.DENIED
        }
    }

    fun updateNotificationAccess(granted: Boolean) {
        _hasNotificationAccess.value = granted
    }

    fun requestCalendarPermission() {
        when (_calendarPermissionState.value) {
            CalendarPermissionUiState.GRANTED -> Unit
            CalendarPermissionUiState.DENIED -> _permissionActions.tryEmit(PermissionAction.RequestCalendarPermission)
            CalendarPermissionUiState.PERMANENTLY_DENIED -> _permissionActions.tryEmit(PermissionAction.OpenAppSettings)
        }
    }

    fun openNotificationSettings() {
        _permissionActions.tryEmit(PermissionAction.OpenNotificationSettings)
    }
}
