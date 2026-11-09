package com.auradock.ui.routes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.auradock.dock.DockUiState
import com.auradock.ui.debug.DebugScreen
import com.auradock.ui.focus.FocusSettingsActions
import com.auradock.ui.focus.FocusSettingsScreen
import com.auradock.ui.focus.FocusSettingsUiState
import com.auradock.ui.focus.VipActions
import com.auradock.ui.focus.VipUiState
import com.auradock.ui.onboarding.OnboardingActions
import com.auradock.ui.onboarding.OnboardingScreen
import com.auradock.ui.onboarding.OnboardingUiState
import com.auradock.ui.onboarding.ScreensaverTestActions
import com.auradock.ui.onboarding.ScreensaverTestScreen
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
    val openScreensaverTest: () -> Unit = {},
    val openDebug: () -> Unit = {},
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
    val weekly by viewModel.weeklyFocus.collectAsState()
    val intention by viewModel.intention.collectAsState()
    val shutdownOffer by viewModel.shutdownOffer.collectAsState()
    val screensaverBanner by viewModel.screensaverBanner.collectAsState()
    val focusOn by viewModel.focusWhileDocked.collectAsState()

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
            undockSummary = summary,
            weeklyFocus = weekly,
            intention = intention,
            offerShutdown = shutdownOffer,
            screensaverBanner = screensaverBanner,
            focusOn = focusOn
        ),
        actions = DashboardActions(
            onEnableWeather = { viewModel.askPermission(SetupPermission.LOCATION) },
            onAllowExactAlarms = { viewModel.askPermission(SetupPermission.EXACT_ALARMS) },
            onSaveTask = { id, title, dueAt -> viewModel.saveTask(id, title, dueAt) },
            onToggleTask = { viewModel.setTaskDone(it, !it.done) },
            onDeleteTask = { viewModel.deleteTask(it) },
            onOpenTemplates = navigation.openTemplates,
            onOpenFocus = navigation.openFocus,
            onOpenSetup = navigation.openSetup,
            onDockNow = { viewModel.dockNow() },
            onEnableCalendarAccess = { viewModel.askPermission(SetupPermission.CALENDAR) },
            onDismissUndockSummary = { viewModel.dismissUndockSummary() },
            onToggleIntention = { viewModel.setIntentionDone(it) },
            onShutdownShown = { viewModel.shutdownShown(it) },
            onSaveTomorrow = { title, dueAt -> viewModel.saveTomorrowTask(title, dueAt) },
            onDismissScreensaverNote = { viewModel.dismissScreensaverNote() },
            onTryScreensaverAgain = {
                viewModel.snoozeScreensaverHint()
                navigation.openScreensaverTest()
            }
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
    val contacts by viewModel.contactsGranted.collectAsState()
    return buildSet {
        if (calendar == CalendarPermissionUiState.GRANTED) add(SetupPermission.CALENDAR)
        if (notificationAccess) add(SetupPermission.NOTIFICATION_ACCESS)
        if (dndAccess) add(SetupPermission.DND_ACCESS)
        if (location == CalendarPermissionUiState.GRANTED) add(SetupPermission.LOCATION)
        if (postNotifications) add(SetupPermission.POST_NOTIFICATIONS)
        if (exactAlarms) add(SetupPermission.EXACT_ALARMS)
        if (contacts) add(SetupPermission.CONTACTS)
    }
}

private fun allow(viewModel: StandbyViewModel, permission: SetupPermission) = viewModel.allow(permission)

@Composable
fun SetupRoute(viewModel: StandbyViewModel, system: SystemSettings, onOpenScreensaverTest: () -> Unit, onOpenDebug: () -> Unit, modifier: Modifier = Modifier) {
    val test by viewModel.screensaverSteps.collectAsState()
    SetupScreen(
        state = SetupUiState(granted = rememberGrantedPermissions(viewModel), screensaverSelected = system.screensaverSelected, screensaverTest = test, version = viewModel.versionLabel),
        actions = SetupActions(
            onAllowPermission = { allow(viewModel, it) },
            onOpenScreensaverSettings = system.openScreensaverSettings,
            onOpenDisplaySettings = system.openDisplaySettings,
            onOpenScreensaverTest = onOpenScreensaverTest,
            onOpenDebug = onOpenDebug
        ),
        modifier = modifier
    )
}

/** The guided screensaver test on its own page (from Setup, and from the dashboard's "Try again"). */
@Composable
fun ScreensaverTestRoute(viewModel: StandbyViewModel, system: SystemSettings, modifier: Modifier = Modifier) {
    val test by viewModel.screensaverSteps.collectAsState()
    ScreensaverTestScreen(
        state = test,
        actions = ScreensaverTestActions(onChoose = system.openScreensaverSettings, onSetTimeout = system.openDisplaySettings),
        modifier = modifier
    )
}

@Composable
fun OnboardingRoute(viewModel: StandbyViewModel, system: SystemSettings, modifier: Modifier = Modifier) {
    val dockFromAnywhere by viewModel.dockFromAnywhere.collectAsState()
    val overlayGranted by viewModel.overlayGranted.collectAsState()
    val test by viewModel.screensaverSteps.collectAsState()
    OnboardingScreen(
        state = OnboardingUiState(
            screensaverTest = test,
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
            screensaverTest = ScreensaverTestActions(onChoose = system.openScreensaverSettings, onSetTimeout = system.openDisplaySettings),
            onToggleDockFromAnywhere = { viewModel.setDockFromAnywhere(it) },
            onAllowOverlay = { viewModel.openOverlaySettings() },
            onPickTemplate = { viewModel.setTemplate(it) },
            onPreviewing = { viewModel.setOnboardingPreviewing(it) },
            onStep = { viewModel.onboardingStep(it) },
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
    val vips by viewModel.vips.collectAsState()
    val ringThrough by viewModel.vipRingThrough.collectAsState()
    val onLoan by viewModel.vipsOnLoan.collectAsState()
    val vipConfirming by viewModel.vipConfirming.collectAsState()

    FocusSettingsScreen(
        state = FocusSettingsUiState(
            focusWhileDocked = focusWhileDocked,
            hasDndAccess = dndAccess,
            hasNotificationAccess = notificationAccess,
            apps = apps,
            pickedApps = picked,
            showMessageText = showText,
            dockFromAnywhere = dockFromAnywhere,
            overlayGranted = overlayGranted,
            vip = VipUiState(vips = vips, ringThrough = ringThrough, onLoan = onLoan, confirming = vipConfirming)
        ),
        actions = FocusSettingsActions(
            onToggleFocus = { viewModel.setFocusWhileDocked(it) },
            onOpenDndSettings = { viewModel.askPermission(SetupPermission.DND_ACCESS) },
            onOpenNotificationAccess = { viewModel.askPermission(SetupPermission.NOTIFICATION_ACCESS) },
            onToggleApp = { viewModel.toggleApp(it) },
            onToggleMessageText = { viewModel.setShowMessageText(it) },
            onToggleDockFromAnywhere = { viewModel.setDockFromAnywhere(it) },
            onAllowOverlay = { viewModel.openOverlaySettings() },
            vip = VipActions(
                onAdd = { viewModel.addVipTapped() },
                onRemove = { viewModel.removeVip(it) },
                onToggleRingThrough = { viewModel.setVipRingThrough(it) },
                onPutBack = { viewModel.putVipsBack() },
                onConfirm = { viewModel.confirmVipRingThrough() },
                onCancelConfirm = { viewModel.cancelVipRingThrough() }
            ),
            loadIcon = { viewModel.appIcon(it) }
        ),
        onOpenSetup = onOpenSetup,
        modifier = modifier
    )
}

/** The hidden debug page: activation numbers from the local event log. */
@Composable
fun DebugRoute(viewModel: StandbyViewModel, modifier: Modifier = Modifier) {
    val summary by androidx.compose.runtime.produceState<com.auradock.analytics.ActivationSummary?>(initialValue = null) { value = viewModel.debugSummary() }
    DebugScreen(summary = summary, version = viewModel.versionLabel, modifier = modifier)
}
