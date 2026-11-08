package com.auradock.ui.routes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.auradock.dock.DockUiState
import com.auradock.ui.focus.FocusSettingsActions
import com.auradock.ui.focus.FocusSettingsScreen
import com.auradock.ui.focus.FocusSettingsUiState
import com.auradock.ui.onboarding.OnboardingActions
import com.auradock.ui.onboarding.OnboardingScreen
import com.auradock.ui.onboarding.OnboardingUiState
import com.auradock.ui.onboarding.SetupActions
import com.auradock.ui.onboarding.SetupPermission
import com.auradock.ui.onboarding.SetupScreen
import com.auradock.ui.onboarding.SetupUiState
import com.auradock.ui.screens.TemplateGalleryScreen
import com.auradock.ui.screens.TemplateGalleryUiState
import com.auradock.ui.screens.dashboard.DashboardActions
import com.auradock.ui.screens.dashboard.DashboardScreen
import com.auradock.ui.screens.dashboard.DashboardUiState
import com.auradock.ui.viewmodel.CalendarPermissionUiState
import com.auradock.ui.viewmodel.StandbyViewModel
import com.auradock.weather.usableAt

/*
 * The "routes": the thin layer between the ViewModel and the screens.
 *
 * A route collects the ViewModel's flows, folds them into the screen's UiState, and maps each screen action onto a ViewModel
 * call. It holds no layout and no logic of its own. The screens themselves (DashboardScreen, FocusSettingsScreen, ...) are
 * stateless, so a redesign only ever touches those, never this file. See docs/DESIGN.md.
 */

/** Things only the Activity can do (open a system settings page), and what it knows about the system. */
data class SystemSettings(
    val screensaverSelected: Boolean,
    val openScreensaverSettings: () -> Unit,
    val openDisplaySettings: () -> Unit
)

/** Moving between the pages that hang off the dashboard. */
data class Navigation(
    val openTemplates: () -> Unit = {},
    val openFocus: () -> Unit = {},
    val openSetup: () -> Unit = {},
    val backToDashboard: () -> Unit = {}
)

@Composable
fun DashboardRoute(viewModel: StandbyViewModel, navigation: Navigation, modifier: Modifier = Modifier) {
    val template by viewModel.currentTemplate.collectAsState()
    val time by viewModel.currentTimeMillis.collectAsState()
    val weather by viewModel.weather.collectAsState()
    val locationPermission by viewModel.locationPermissionState.collectAsState()
    val events by viewModel.todayEvents.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val exactAlarmsAllowed by viewModel.exactAlarmsAllowed.collectAsState()
    val calendarPermission by viewModel.calendarPermissionState.collectAsState()
    val summary by viewModel.undockSummary.collectAsState()

    DashboardScreen(
        state = DashboardUiState(
            template = template,
            timeMillis = time,
            weather = weather.usableAt(time),
            locationPermission = locationPermission,
            events = events,
            tasks = tasks,
            exactAlarmsAllowed = exactAlarmsAllowed,
            calendarPermission = calendarPermission,
            undockSummary = summary
        ),
        actions = DashboardActions(
            onEnableWeather = { viewModel.requestLocationPermission() },
            onAllowExactAlarms = { viewModel.openExactAlarmSettings() },
            onSaveTask = { id, title, dueAt -> viewModel.saveTask(id, title, dueAt) },
            onToggleTask = { viewModel.setTaskDone(it, !it.done) },
            onDeleteTask = { viewModel.deleteTask(it) },
            onOpenTemplates = navigation.openTemplates,
            onOpenFocus = navigation.openFocus,
            onOpenSetup = navigation.openSetup,
            onDockNow = { viewModel.enterDock() },
            onEnableCalendarAccess = { viewModel.requestCalendarPermission() },
            onDismissUndockSummary = { viewModel.dismissUndockSummary() }
        ),
        modifier = modifier
    )
}

@Composable
private fun rememberGalleryState(viewModel: StandbyViewModel): TemplateGalleryUiState {
    val template by viewModel.currentTemplate.collectAsState()
    val time by viewModel.currentTimeMillis.collectAsState()
    val weather by viewModel.weather.collectAsState()
    val events by viewModel.todayEvents.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    return TemplateGalleryUiState(
        selected = template,
        dockState = DockUiState(time, weather.usableAt(time), events.orEmpty(), tasks.orEmpty())
    )
}

@Composable
fun TemplateGalleryRoute(viewModel: StandbyViewModel, navigation: Navigation, modifier: Modifier = Modifier) {
    TemplateGalleryScreen(
        state = rememberGalleryState(viewModel),
        onApply = {
            viewModel.setTemplate(it)
            navigation.backToDashboard()
        },
        modifier = modifier
    )
}

/** The permissions that are allowed right now. Each flow is re-checked when the app resumes (see MainActivity.onResume). */
@Composable
private fun rememberGrantedPermissions(viewModel: StandbyViewModel): Set<SetupPermission> {
    val calendar by viewModel.calendarPermissionState.collectAsState()
    val notificationAccess by viewModel.hasNotificationAccess.collectAsState()
    val dndAccess by viewModel.dndAccessGranted.collectAsState()
    val location by viewModel.locationPermissionState.collectAsState()
    val postNotifications by viewModel.postNotificationsGranted.collectAsState()
    val exactAlarms by viewModel.exactAlarmsAllowed.collectAsState()
    return buildSet {
        if (calendar == CalendarPermissionUiState.GRANTED) add(SetupPermission.CALENDAR)
        if (notificationAccess) add(SetupPermission.NOTIFICATION_ACCESS)
        if (dndAccess) add(SetupPermission.DND_ACCESS)
        if (location == CalendarPermissionUiState.GRANTED) add(SetupPermission.LOCATION)
        if (postNotifications) add(SetupPermission.POST_NOTIFICATIONS)
        if (exactAlarms) add(SetupPermission.EXACT_ALARMS)
    }
}

private fun allow(viewModel: StandbyViewModel, permission: SetupPermission) {
    when (permission) {
        SetupPermission.CALENDAR -> viewModel.requestCalendarPermission()
        SetupPermission.NOTIFICATION_ACCESS -> viewModel.openNotificationSettings()
        SetupPermission.DND_ACCESS -> viewModel.openDndAccessSettings()
        SetupPermission.LOCATION -> viewModel.requestLocationPermission()
        SetupPermission.POST_NOTIFICATIONS -> viewModel.requestPostNotifications()
        SetupPermission.EXACT_ALARMS -> viewModel.openExactAlarmSettings()
    }
}

@Composable
fun SetupRoute(viewModel: StandbyViewModel, system: SystemSettings, modifier: Modifier = Modifier) {
    SetupScreen(
        state = SetupUiState(granted = rememberGrantedPermissions(viewModel), screensaverSelected = system.screensaverSelected),
        actions = SetupActions(
            onAllowPermission = { allow(viewModel, it) },
            onOpenScreensaverSettings = system.openScreensaverSettings,
            onOpenDisplaySettings = system.openDisplaySettings
        ),
        modifier = modifier
    )
}

@Composable
fun OnboardingRoute(viewModel: StandbyViewModel, system: SystemSettings, modifier: Modifier = Modifier) {
    val dockFromAnywhere by viewModel.dockFromAnywhere.collectAsState()
    val overlayGranted by viewModel.overlayGranted.collectAsState()
    OnboardingScreen(
        state = OnboardingUiState(
            granted = rememberGrantedPermissions(viewModel),
            screensaverSelected = system.screensaverSelected,
            dockFromAnywhere = dockFromAnywhere,
            overlayGranted = overlayGranted,
            gallery = rememberGalleryState(viewModel)
        ),
        actions = OnboardingActions(
            onAllowPermission = { allow(viewModel, it) },
            onOpenScreensaverSettings = system.openScreensaverSettings,
            onOpenDisplaySettings = system.openDisplaySettings,
            onToggleDockFromAnywhere = { viewModel.setDockFromAnywhere(it) },
            onAllowOverlay = { viewModel.openOverlaySettings() },
            onPickTemplate = { viewModel.setTemplate(it) },
            onFinish = { viewModel.finishOnboarding() }
        ),
        modifier = modifier
    )
}

@Composable
fun FocusSettingsRoute(viewModel: StandbyViewModel, modifier: Modifier = Modifier, onOpenSetup: () -> Unit = {}) {
    LaunchedEffect(Unit) { viewModel.loadApps() }
    val focusWhileDocked by viewModel.focusWhileDocked.collectAsState()
    val dndAccess by viewModel.dndAccessGranted.collectAsState()
    val notificationAccess by viewModel.hasNotificationAccess.collectAsState()
    val apps by viewModel.apps.collectAsState()
    val picked by viewModel.pickedApps.collectAsState()
    val showText by viewModel.showMessageText.collectAsState()
    val dockFromAnywhere by viewModel.dockFromAnywhere.collectAsState()
    val overlayGranted by viewModel.overlayGranted.collectAsState()

    FocusSettingsScreen(
        state = FocusSettingsUiState(
            focusWhileDocked = focusWhileDocked,
            hasDndAccess = dndAccess,
            hasNotificationAccess = notificationAccess,
            apps = apps,
            pickedApps = picked,
            showMessageText = showText,
            dockFromAnywhere = dockFromAnywhere,
            overlayGranted = overlayGranted
        ),
        actions = FocusSettingsActions(
            onToggleFocus = { viewModel.setFocusWhileDocked(it) },
            onOpenDndSettings = { viewModel.openDndAccessSettings() },
            onOpenNotificationAccess = { viewModel.openNotificationSettings() },
            onToggleApp = { viewModel.toggleApp(it) },
            onToggleMessageText = { viewModel.setShowMessageText(it) },
            onToggleDockFromAnywhere = { viewModel.setDockFromAnywhere(it) },
            onAllowOverlay = { viewModel.openOverlaySettings() },
            loadIcon = { viewModel.appIcon(it) }
        ),
        onOpenSetup = onOpenSetup,
        modifier = modifier
    )
}
