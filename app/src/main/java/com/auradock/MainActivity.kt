package com.auradock

import android.Manifest
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.app.ActivityCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.auradock.dock.DockData
import com.auradock.dock.DockScreen
import com.auradock.dock.DockUiState
import com.auradock.focus.DockFocusController
import com.auradock.focus.DockSource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import com.auradock.weather.usableAt
import com.auradock.permissions.isNotificationAccessEnabled
import com.auradock.permissions.openAppSettings
import com.auradock.permissions.openDndAccessSettings
import com.auradock.permissions.openExactAlarmSettings
import com.auradock.ui.focus.FocusSettingsScreen
import com.auradock.permissions.openNotificationAccessSettings
import com.auradock.permissions.openScreensaverSettings
import com.auradock.ui.onboarding.OnboardingScreen
import com.auradock.ui.screens.TemplateGalleryScreen
import com.auradock.ui.screens.dashboard.DashboardScreen
import com.auradock.ui.theme.AuraDockTheme
import com.auradock.ui.viewmodel.PermissionAction
import com.auradock.ui.viewmodel.StandbyViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val standbyViewModel: StandbyViewModel by viewModels()

    @Inject lateinit var dockData: DockData
    @Inject lateinit var dockFocus: DockFocusController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Do Not Disturb follows the dock: on while it is showing and this screen is visible, off when it closes or the
        // app goes to the background. The finally block also runs if the Activity is destroyed while docked.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                try {
                    standbyViewModel.dockVisible.collect { dockFocus.setActive(DockSource.APP, it) }
                } finally {
                    dockFocus.setActive(DockSource.APP, false)
                }
            }
        }

        setContent {
            AuraDockTheme {
                val currentTemplate by standbyViewModel.currentTemplate.collectAsState()
                val timeMillis by standbyViewModel.currentTimeMillis.collectAsState()
                val dockVisible by standbyViewModel.dockVisible.collectAsState()
                val manualDock by standbyViewModel.manualDock.collectAsState()
                val todayEvents by standbyViewModel.todayEvents.collectAsState()
                val tasks by standbyViewModel.tasks.collectAsState()
                val exactAlarmsAllowed by standbyViewModel.exactAlarmsAllowed.collectAsState()
                val weather by standbyViewModel.weather.collectAsState()
                val focusWhileDocked by standbyViewModel.focusWhileDocked.collectAsState()
                val dndAccessGranted by standbyViewModel.dndAccessGranted.collectAsState()
                val hasNotificationAccess by standbyViewModel.hasNotificationAccess.collectAsState()
                val apps by standbyViewModel.apps.collectAsState()
                val pickedApps by standbyViewModel.pickedApps.collectAsState()
                val showMessageText by standbyViewModel.showMessageText.collectAsState()
                val locationPermissionState by standbyViewModel.locationPermissionState.collectAsState()
                val calendarPermissionState by standbyViewModel.calendarPermissionState.collectAsState()
                val onboardingDone by standbyViewModel.onboardingDone.collectAsState()
                // which page is open: "dashboard", "templates" or "focus"
                var page by rememberSaveable { mutableStateOf("dashboard") }
                LaunchedEffect(page) { if (page == "focus") standbyViewModel.loadApps() }
                val calendarPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { granted ->
                    val permanentlyDenied = !granted &&
                        !ActivityCompat.shouldShowRequestPermissionRationale(
                            this@MainActivity,
                            Manifest.permission.READ_CALENDAR
                        )
                    standbyViewModel.onCalendarPermissionResult(granted, permanentlyDenied)
                }

                val locationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { granted ->
                    val permanentlyDenied = !granted &&
                        !ActivityCompat.shouldShowRequestPermissionRationale(
                            this@MainActivity,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    standbyViewModel.onLocationPermissionResult(granted, permanentlyDenied)
                }
                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { /* nothing to do: without it, reminders are saved but no notification is shown */ }

                LaunchedEffect(Unit) {
                    standbyViewModel.permissionActions.collect { action ->
                        when (action) {
                            PermissionAction.RequestCalendarPermission -> {
                                calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                            }

                            PermissionAction.OpenNotificationSettings -> {
                                openNotificationAccessSettings(this@MainActivity)
                            }

                            PermissionAction.OpenAppSettings -> {
                                openAppSettings(this@MainActivity)
                            }

                            PermissionAction.RequestLocationPermission -> {
                                locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                            }

                            PermissionAction.RequestNotificationPermission -> {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }

                            PermissionAction.OpenDndAccessSettings -> {
                                openDndAccessSettings(this@MainActivity)
                            }

                            PermissionAction.OpenExactAlarmSettings -> {
                                openExactAlarmSettings(this@MainActivity)
                            }
                        }
                    }
                }

                ManualDockOrientation(manualDock)
                DockWindowMode(dockVisible)
                BackHandler(enabled = dockVisible) { standbyViewModel.dismissDock() }
                BackHandler(enabled = page != "dashboard" && !dockVisible) { page = "dashboard" }

                val targetState = when {
                    dockVisible -> "dock"
                    !onboardingDone -> "onboarding"
                    page == "templates" -> "gallery"
                    page == "focus" -> "focus"
                    else -> "dashboard"
                }

                AnimatedContent(
                    targetState = targetState,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(260)) + scaleIn(initialScale = 0.985f, animationSpec = tween(260)))
                            .togetherWith(
                                fadeOut(animationSpec = tween(200)) + scaleOut(targetScale = 0.99f, animationSpec = tween(200))
                            )
                    },
                    label = "rootModeTransition"
                ) { mode ->
                    when (mode) {
                        // A touch anywhere closes the dock, like it does in the screensaver.
                        "dock" -> Box(
                            Modifier
                                .fillMaxSize()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { standbyViewModel.dismissDock() }
                        ) {
                            DockScreen(dockData = dockData, window = this@MainActivity.window)
                        }

                        "onboarding" -> OnboardingScreen(
                            template = currentTemplate,
                            dockState = DockUiState(timeMillis, weather.usableAt(timeMillis), todayEvents.orEmpty(), tasks.orEmpty()),
                            onOpenScreensaverSettings = { openScreensaverSettings(this@MainActivity) },
                            onPickTemplate = { standbyViewModel.setTemplate(it) },
                            onFinish = { standbyViewModel.finishOnboarding() }
                        )

                        "gallery" -> TemplateGalleryScreen(
                            selected = currentTemplate,
                            state = DockUiState(timeMillis, weather.usableAt(timeMillis), todayEvents.orEmpty(), tasks.orEmpty()),
                            onApplyClick = {
                                standbyViewModel.setTemplate(it)
                                page = "dashboard"
                            }
                        )

                        "focus" -> FocusSettingsScreen(
                            focusWhileDocked = focusWhileDocked,
                            hasDndAccess = dndAccessGranted,
                            onToggleFocus = { standbyViewModel.setFocusWhileDocked(it) },
                            onOpenDndSettings = { standbyViewModel.openDndAccessSettings() },
                            hasNotificationAccess = hasNotificationAccess,
                            onOpenNotificationAccess = { standbyViewModel.openNotificationSettings() },
                            apps = apps,
                            pickedApps = pickedApps,
                            onToggleApp = { standbyViewModel.toggleApp(it) },
                            showMessageText = showMessageText,
                            onToggleMessageText = { standbyViewModel.setShowMessageText(it) },
                            loadIcon = { standbyViewModel.appIcon(it) }
                        )

                        else -> DashboardScreen(
                            currentTemplate = currentTemplate,
                            timeMillis = timeMillis,
                            weather = weather.usableAt(timeMillis),
                            locationPermissionState = locationPermissionState,
                            onEnableWeather = { standbyViewModel.requestLocationPermission() },
                            todayEvents = todayEvents,
                            tasks = tasks,
                            exactAlarmsAllowed = exactAlarmsAllowed,
                            onAllowExactAlarms = { standbyViewModel.openExactAlarmSettings() },
                            onSaveTask = { id, title, dueAt -> standbyViewModel.saveTask(id, title, dueAt) },
                            onToggleTask = { standbyViewModel.setTaskDone(it, !it.done) },
                            onDeleteTask = { standbyViewModel.deleteTask(it) },
                            calendarPermissionState = calendarPermissionState,
                            onOpenTemplates = { page = "templates" },
                            onOpenFocus = { page = "focus" },
                            onDockNow = { standbyViewModel.enterDock() },
                            onEnableCalendarAccess = { standbyViewModel.requestCalendarPermission() }
                        )
                    }
                }
            }
        }
    }

    /**
     * "Dock now" forces landscape (either way up, following the sensor) so the dock fills the screen even
     * when the phone is held upright. On exit it goes back to portrait for the dashboard, and only once the
     * screen really is portrait does it hand rotation back to the system, so the dock does not instantly
     * re-open just because the phone is still leaning in landscape.
     */
    @androidx.compose.runtime.Composable
    private fun ManualDockOrientation(manualDock: Boolean) {
        val orientation = LocalConfiguration.current.orientation
        var returningToPortrait by rememberSaveable { mutableStateOf(false) }
        LaunchedEffect(manualDock, orientation) {
            when {
                manualDock -> {
                    returningToPortrait = true
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                }
                returningToPortrait && orientation == Configuration.ORIENTATION_PORTRAIT -> {
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    returningToPortrait = false
                }
                returningToPortrait -> requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }
    }

    /** While the dock shows: keep the screen on and hide the system bars, as the screensaver does. */
    @androidx.compose.runtime.Composable
    private fun DockWindowMode(dockVisible: Boolean) {
        DisposableEffect(dockVisible) {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            if (dockVisible) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(WindowInsetsCompat.Type.systemBars())
            }
            onDispose {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    override fun onStart() {
        super.onStart()
        standbyViewModel.refreshCalendarPermission()
        standbyViewModel.updateNotificationAccess(isNotificationAccessEnabled(this))
        standbyViewModel.refreshLocationPermission()
        standbyViewModel.refreshWeather() // the app is visible: refreshes only if the weather is 30+ minutes old
    }

    override fun onResume() {
        super.onResume()
        standbyViewModel.refreshExactAlarmAccess() // the user may have just allowed it in settings
        standbyViewModel.refreshDndAccess()        // ...or just allowed Do Not Disturb access
        standbyViewModel.refreshCalendarPermission()
        standbyViewModel.updateNotificationAccess(isNotificationAccessEnabled(this))
    }
}
