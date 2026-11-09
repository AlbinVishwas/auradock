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
import com.auradock.analytics.AnalyticsEvents
import com.auradock.dock.DockTap
import com.auradock.dock.tapOnDock
import com.auradock.ui.theme.AuraDockMotion
import com.auradock.permissions.PermissionPrompts
import com.auradock.ui.onboarding.SetupPermission
import com.auradock.focus.DockFocusController
import com.auradock.focus.vip.Vip
import com.auradock.focus.vip.VipContacts
import com.auradock.focus.FocusSettings
import com.auradock.onboarding.OnboardingState
import com.auradock.permissions.canScheduleExactAlarms
import com.auradock.dock.DockTemplate
import com.auradock.dock.TemplateRepository
import com.auradock.ui.theme.BoldPalette
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
    private val splitRepository: com.auradock.dock.split.SplitRepository,
    private val taskRepository: TaskRepository,
    private val weatherRepository: WeatherRepository,
    private val focusSettings: FocusSettings,
    private val dndController: DndController,
    private val dockFocus: DockFocusController,
    private val vipContacts: VipContacts,
    private val screensaverTest: com.auradock.screensaver.ScreensaverTest,
    private val events: com.auradock.analytics.EventLog,
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
            events.log(AnalyticsEvents.SHUTDOWN_USED)
        }
        if (dueAt != null) askOnFirstUse(*PermissionPrompts.forTimedTask(grantedPermissions(), askedPermissions()).toTypedArray())
    }
    private fun minuteClockNow() = currentTimeMillis.value

    /** Today's morning intention for the top of the Today card (null when there is none). */
    val intention: StateFlow<IntentionLine?> = intentions.today.map { intentionLine(it.entry) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    fun setIntentionDone(done: Boolean) { intentions.setDone(done) }

    /** The guided screensaver test: which of its three steps are done, and the note the dashboard shows about it. */
    val screensaverSteps: StateFlow<com.auradock.screensaver.ScreensaverTestUiState> = screensaverTest.steps
    val screensaverBanner: StateFlow<com.auradock.screensaver.ScreensaverBanner> = screensaverTest.banner
    fun dismissScreensaverNote() = screensaverTest.dismissBanner()
    fun snoozeScreensaverHint() = screensaverTest.snoozeHint()

    /** The card shown after a focus session of two minutes or more, until it is dismissed. */
    val undockSummary: StateFlow<SessionSummary?> = sessions.summary
    fun dismissUndockSummary() = sessions.dismissSummary()


    /** Current time, refreshed at the start of every minute. Shared by the clock, preview and dashboard date. */
    val currentTimeMillis: StateFlow<Long> = minuteClock.minutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), minuteClock.currentMillis())

    /** The saved template choice (shared with the screensaver). */
    val currentTemplate: StateFlow<DockTemplate> = templateRepository.selected

    /** The Split dock's stacks (the gallery and the dashboard draw its card with them). */
    val splitStacks: StateFlow<com.auradock.dock.split.SplitStacks> = splitRepository.stacks

    /** The World dock: where the weather was read (home) and the extra cities picked in the gallery. */
    val worldHome: StateFlow<com.auradock.dock.world.GeoPoint?> = weatherRepository.place
    val worldCities: StateFlow<List<com.auradock.dock.world.WorldCity>> = templateRepository.worldCities
    fun setWorldCities(cities: List<com.auradock.dock.world.WorldCity>) = templateRepository.setWorldCities(cities)

    /** The Bold clock's colourway (saved; the gallery previews it live as each dot is tapped). */
    val boldPalette: StateFlow<BoldPalette> = templateRepository.boldPalette
    fun setBoldPalette(palette: BoldPalette) = templateRepository.selectBoldPalette(palette)

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
        if (dueAt != null) askOnFirstUse(*PermissionPrompts.forTimedTask(grantedPermissions(), askedPermissions()).toTypedArray())
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

    // ── Permission sheets: each permission is asked when its feature is first used ──────────────

    private val promptPrefs = context.getSharedPreferences("auradock_prefs", Context.MODE_PRIVATE)
    private val promptQueue = MutableStateFlow<List<SetupPermission>>(emptyList())

    /** The permission whose card is showing as a sheet right now, or null. */
    val permissionPrompt: StateFlow<SetupPermission?> = promptQueue.map { it.firstOrNull() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** The permissions AuraDock has already asked about (the first-use sheet shows only once for each). */
    private fun askedPermissions(): Set<String> {
        val asked = promptPrefs.getStringSet("permissions_asked", emptySet()).orEmpty()
        return if (promptPrefs.getBoolean("asked_post_notifications", false)) asked + SetupPermission.POST_NOTIFICATIONS.name else asked
    }

    private fun markAsked(permission: SetupPermission) {
        promptPrefs.edit().putStringSet("permissions_asked", askedPermissions() + permission.name).apply()
    }

    fun isGranted(permission: SetupPermission): Boolean = when (permission) {
        SetupPermission.CALENDAR -> _calendarPermissionState.value == CalendarPermissionUiState.GRANTED
        SetupPermission.NOTIFICATION_ACCESS -> hasNotificationAccess.value
        SetupPermission.DND_ACCESS -> _dndAccessGranted.value
        SetupPermission.LOCATION -> _locationPermissionState.value == CalendarPermissionUiState.GRANTED
        SetupPermission.POST_NOTIFICATIONS -> _postNotificationsGranted.value
        SetupPermission.EXACT_ALARMS -> _exactAlarmsAllowed.value
        SetupPermission.CONTACTS -> vipContacts.canRead()
    }

    /** Records each permission the first time it is seen allowed (from the sheet, from Setup or from Android's own settings). */
    fun noteGrantedPermissions() {
        val logged = promptPrefs.getStringSet("permissions_logged", emptySet()).orEmpty()
        val fresh = grantedPermissions().map { it.name }.toSet() - logged
        if (fresh.isEmpty()) return
        fresh.forEach { events.log(AnalyticsEvents.PERMISSION_GRANTED, it) }
        promptPrefs.edit().putStringSet("permissions_logged", logged + fresh).apply()
    }

    private fun grantedPermissions(): Set<SetupPermission> = SetupPermission.entries.filter { isGranted(it) }.toSet()

    private fun enqueue(permission: SetupPermission) {
        promptQueue.update { if (permission in it) it else it + permission }
    }

    /** The user tapped an "Allow" for a feature: its permission card shows as a sheet (unless it is already allowed). */
    fun askPermission(permission: SetupPermission) {
        if (!isGranted(permission)) enqueue(permission)
    }

    /** A feature is being used for the first time: show the card once, never again by itself. */
    private fun askOnFirstUse(vararg permissions: SetupPermission) {
        permissions.filter { PermissionPrompts.askOnFirstUse(it, isGranted(it), askedPermissions()) }.forEach(::enqueue)
    }

    /** The sheet was answered: "Allow" goes on to Android, "Skip" just closes it. */
    fun answerPrompt(allow: Boolean) {
        val permission = promptQueue.value.firstOrNull() ?: return
        markAsked(permission)
        if (!allow) events.log(AnalyticsEvents.PERMISSION_SKIPPED, permission.name)
        promptQueue.update { it.drop(1) }
        if (allow) allow(permission, pickAfterGrant = permission == SetupPermission.CONTACTS)
        if (promptQueue.value.isEmpty() && dockAfterPrompt) {
            dockAfterPrompt = false
            enterDock()
        }
    }

    /** Goes straight to Android's own prompt or settings page for [permission] (used by the sheet and by the Setup page). */
    fun allow(permission: SetupPermission, pickAfterGrant: Boolean = false) {
        when (permission) {
            SetupPermission.CALENDAR -> requestCalendarPermission()
            SetupPermission.NOTIFICATION_ACCESS -> openNotificationSettings()
            SetupPermission.DND_ACCESS -> openDndAccessSettings()
            SetupPermission.LOCATION -> requestLocationPermission()
            SetupPermission.POST_NOTIFICATIONS -> requestPostNotifications()
            SetupPermission.EXACT_ALARMS -> openExactAlarmSettings()
            SetupPermission.CONTACTS -> {
                pickAfterContactsGrant = pickAfterGrant
                _permissionActions.tryEmit(PermissionAction.RequestContactsRead)
            }
        }
    }

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
        if (vipContacts.canRead()) _permissionActions.tryEmit(PermissionAction.PickContact) else askPermission(SetupPermission.CONTACTS)
    }

    /** After a contacts permission from "Add a person" the picker opens; from Setup it does not. */
    private var pickAfterContactsGrant = false

    fun onContactsReadResult(granted: Boolean, permanentlyDenied: Boolean) {
        _contactsGranted.value = vipContacts.canRead()
        when {
            granted && pickAfterContactsGrant -> _permissionActions.tryEmit(PermissionAction.PickContact)
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

    private val _vipConfirming = MutableStateFlow(false)

    /** True while the one-time "Favourites can sync" sheet is showing. */
    val vipConfirming: StateFlow<Boolean> = _vipConfirming.asStateFlow()

    /** "Turn on": the sheet is only ever shown once; then the switch goes on (after the contacts permission, if still needed). */
    fun confirmVipRingThrough() {
        focusSettings.markVipConfirmed()
        _vipConfirming.value = false
        setVipRingThrough(true)
    }

    fun cancelVipRingThrough() {
        _vipConfirming.value = false
    }

    /** The first time it is switched on, a sheet explains the Favourites mark; after that the switch just works. */
    fun setVipRingThrough(enabled: Boolean) {
        if (enabled && !focusSettings.vipConfirmed.value) {
            _vipConfirming.value = true
            return
        }
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

    private val _contactsGranted = MutableStateFlow(vipContacts.canRead())

    /** True while the contacts permission is allowed (for the Setup page). */
    val contactsGranted: StateFlow<Boolean> = _contactsGranted.asStateFlow()

    /** The user may have just changed the contacts permissions in system settings. */
    fun refreshContactsAccess() {
        _contactsGranted.value = vipContacts.canRead()
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
        if (!hasNotificationAccess.value) askOnFirstUse(SetupPermission.NOTIFICATION_ACCESS) // the dock strip needs it
    }

    suspend fun appIcon(packageName: String): androidx.compose.ui.graphics.ImageBitmap? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        appCatalog.icon(packageName)?.toBitmap(96, 96)?.asImageBitmap()
    }

    /** False until the first-launch walkthrough has been finished. */
    val onboardingDone: StateFlow<Boolean> = onboardingState.done

    fun finishOnboarding() {
        events.log(AnalyticsEvents.ONBOARDING_STEP, "done")
        onboardingState.complete()
    }

    /** The walkthrough moved to [step] (welcome, preview, template, screensaver, anywhere). */
    fun onboardingStep(step: String) = events.log(AnalyticsEvents.ONBOARDING_STEP, step)

    private val _onboardingPreviewing = MutableStateFlow(false)

    /** True while the first-run dock preview shows: the window turns landscape and the system bars hide, like a real dock. */
    val onboardingPreviewing: StateFlow<Boolean> = _onboardingPreviewing.asStateFlow()

    fun setOnboardingPreviewing(previewing: Boolean) {
        _onboardingPreviewing.value = previewing
    }

    fun enterDock(source: String = "dock_now") {
        com.auradock.monitoring.Monitoring.markDockStart(source)
        dockMode.update { it.enterManual() }
    }

    private val _undoFocusShowing = MutableStateFlow(false)

    /** True for a few seconds after a touch on the dock while Focus is on: the "Undo focus" control shows. */
    val undoFocusShowing: StateFlow<Boolean> = _undoFocusShowing.asStateFlow()
    private var undoHideJob: kotlinx.coroutines.Job? = null

    /** A touch on the in-app dock: the first shows "Undo focus" (if there is Focus to undo), the next closes the dock. */
    /** A double tap leaves the dock (a single tap never does). */
    fun onDockDoubleTapped() {
        undoHideJob?.cancel()
        _undoFocusShowing.value = false
        dismissDock()
    }

    fun onDockTapped() {
        when (tapOnDock(dockFocus.focusOn.value, _undoFocusShowing.value)) {
            DockTap.SHOW_UNDO -> {
                _undoFocusShowing.value = true
                undoHideJob?.cancel()
                undoHideJob = viewModelScope.launch {
                    kotlinx.coroutines.delay(AuraDockMotion.UndoFocus.toLong())
                    _undoFocusShowing.value = false
                }
            }
            DockTap.NONE -> Unit
        }
    }

    /** "Undo focus": Do Not Disturb goes back to how it was, for this dock session; the dock stays open. */
    fun undoFocus() {
        undoHideJob?.cancel()
        _undoFocusShowing.value = false
        events.log(AnalyticsEvents.FOCUS_UNDONE)
        dockFocus.undoFocus()
    }

    private var dockAfterPrompt = false

    /**
     * "Dock now". The first time, with Focus on and no Do Not Disturb access, the permission card comes first as a sheet; the dock
     * opens as soon as it is answered.
     */
    fun dockNow() {
        val ask = PermissionPrompts.forFirstDock(focusSettings.focusWhileDocked.value, grantedPermissions(), askedPermissions())
        if (ask.isEmpty()) {
            enterDock()
        } else {
            dockAfterPrompt = true
            askOnFirstUse(*ask.toTypedArray())
        }
    }
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
        events.log(AnalyticsEvents.TEMPLATE_CHANGED, template.name)
    }

    /** "Share crash reports" (Setup): on by default; turning it off stops all Sentry sending at once. */
    private val _shareCrashReports = MutableStateFlow(com.auradock.monitoring.Monitoring.isSharing(context))
    val shareCrashReports: StateFlow<Boolean> = _shareCrashReports.asStateFlow()
    fun setShareCrashReports(share: Boolean) {
        _shareCrashReports.value = share
        com.auradock.monitoring.Monitoring.setSharing(context.applicationContext as android.app.Application, share)
    }

    /** Debug builds only (the debug page's button): sends a test crash and a non-fatal report to the Sentry dev environment. */
    fun sendTestEvents() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) { com.auradock.monitoring.Monitoring.sendTestEvents(context.applicationContext as android.app.Application) }
    }

    /** The version shown at the bottom of Setup, e.g. "0.2.2 (8)". */
    val versionLabel: String = runCatching {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        @Suppress("DEPRECATION") "${info.versionName} (${info.versionCode})"
    }.getOrDefault("")

    /** The numbers for the hidden debug page, worked out from the local event log. */
    suspend fun debugSummary(): com.auradock.analytics.ActivationSummary {
        val installedAt = runCatching { context.packageManager.getPackageInfo(context.packageName, 0).firstInstallTime }.getOrDefault(0L)
        return com.auradock.analytics.activationSummary(events.events(), installedAt, System.currentTimeMillis())
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
