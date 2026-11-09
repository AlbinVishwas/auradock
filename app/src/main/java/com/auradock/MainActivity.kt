package com.auradock

import com.auradock.ui.theme.AuraDockSizes
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxHeight
import android.Manifest
import android.content.pm.ActivityInfo
import android.os.Build
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
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
import com.auradock.permissions.isAuraDockScreensaver
import com.auradock.permissions.isNotificationAccessEnabled
import com.auradock.permissions.openDisplaySettings
import com.auradock.permissions.openAppSettings
import com.auradock.permissions.openDndAccessSettings
import com.auradock.permissions.openExactAlarmSettings
import com.auradock.permissions.openOverlaySettings
import com.auradock.ui.routes.DashboardRoute
import com.auradock.ui.routes.FocusSettingsRoute
import com.auradock.ui.routes.Navigation
import com.auradock.ui.routes.OnboardingRoute
import com.auradock.ui.routes.DebugRoute
import com.auradock.ui.routes.ScreensaverTestRoute
import com.auradock.ui.routes.SetupRoute
import com.auradock.ui.routes.SystemSettings
import com.auradock.ui.routes.TemplateGalleryRoute
import com.auradock.permissions.openNotificationAccessSettings
import com.auradock.permissions.openScreensaverSettings
import com.auradock.ui.onboarding.OnboardingScreen
import com.auradock.ui.onboarding.PermissionSheet
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import com.auradock.ui.theme.AuraDockSpacing
import com.auradock.dock.UndoFocusPill
import com.auradock.dock.DockTouch
import com.auradock.ui.onboarding.SetupPermission
import com.auradock.ui.onboarding.SetupScreen
import com.auradock.ui.viewmodel.CalendarPermissionUiState
import com.auradock.ui.screens.TemplateGalleryScreen
import com.auradock.ui.screens.dashboard.DashboardScreen
import com.auradock.ui.components.DockMorphHost
import com.auradock.ui.theme.AuraDockMotion
import com.auradock.ui.theme.AuraDockTheme
import com.auradock.ui.viewmodel.PermissionAction
import com.auradock.ui.viewmodel.StandbyViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val standbyViewModel: StandbyViewModel by viewModels()

    /** Whether Android has AuraDock as the chosen screensaver; re-read in onResume. */
    private var screensaverSelected by androidx.compose.runtime.mutableStateOf(false)

    @Inject lateinit var dockData: DockData
    @Inject lateinit var dockFocus: DockFocusController
    @Inject lateinit var screensaverTest: com.auradock.screensaver.ScreensaverTest
    @Inject lateinit var sessions: com.auradock.sessions.SessionTracker

    override fun onCreate(savedInstanceState: Bundle?) {
        // Edge to edge on every Android version (Android 15+ forces it): the window reaches under transparent bars and each screen pads itself
        // with the insets (see ui/theme/Insets.kt). The dock hides the bars altogether.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) openDockIfAsked(intent)

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
                val dockVisible by standbyViewModel.dockVisible.collectAsState()
                val onboardingDone by standbyViewModel.onboardingDone.collectAsState()
                // which page is open: "dashboard", "templates", "focus" or "setup"
                var page by rememberSaveable { mutableStateOf("dashboard") }
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
                ) { granted ->
                    val permanentlyDenied = !granted &&
                        !ActivityCompat.shouldShowRequestPermissionRationale(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS)
                    standbyViewModel.onPostNotificationsResult(granted, permanentlyDenied)
                }

                val contactsReadLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { granted ->
                    val permanentlyDenied = !granted &&
                        !ActivityCompat.shouldShowRequestPermissionRationale(this@MainActivity, Manifest.permission.READ_CONTACTS)
                    standbyViewModel.onContactsReadResult(granted, permanentlyDenied)
                }
                val contactsWriteLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { granted ->
                    val permanentlyDenied = !granted &&
                        !ActivityCompat.shouldShowRequestPermissionRationale(this@MainActivity, Manifest.permission.WRITE_CONTACTS)
                    standbyViewModel.onContactsWriteResult(granted, permanentlyDenied)
                }
                val contactPicker = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.PickContact()
                ) { uri -> if (uri != null) standbyViewModel.onContactPicked(uri) }

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

                            PermissionAction.OpenOverlaySettings -> {
                                openOverlaySettings(this@MainActivity)
                            }

                            PermissionAction.OpenExactAlarmSettings -> {
                                openExactAlarmSettings(this@MainActivity)
                            }

                            PermissionAction.RequestContactsRead -> contactsReadLauncher.launch(Manifest.permission.READ_CONTACTS)
                            PermissionAction.RequestContactsWrite -> contactsWriteLauncher.launch(Manifest.permission.WRITE_CONTACTS)
                            PermissionAction.PickContact -> contactPicker.launch(null)
                        }
                    }
                }

                val previewing by standbyViewModel.onboardingPreviewing.collectAsState()
                val permissionPrompt by standbyViewModel.permissionPrompt.collectAsState()
                DockOrientation(dockVisible || previewing)
                DockWindowMode(dockVisible || previewing)
                BackHandler(enabled = dockVisible) { standbyViewModel.dismissDock() }
                BackHandler(enabled = page != "dashboard" && !dockVisible) { page = "dashboard" }

                val targetState = when {
                    dockVisible -> "dock"
                    !onboardingDone -> "onboarding"
                    page == "templates" -> "gallery"
                    page == "focus" -> "focus"
                    page == "setup" -> "setup"
                    page == "screensaver" -> "screensaver"
                    page == "debug" -> "debug"
                    else -> "dashboard"
                }

                // A trace for each screen opening (ends once it has drawn): codes only, sampled with the rest of the traces.
                LaunchedEffect(targetState) {
                    val span = com.auradock.monitoring.Monitoring.startScreenLoad(targetState)
                    androidx.compose.runtime.withFrameNanos { }
                    androidx.compose.runtime.withFrameNanos { }
                    com.auradock.monitoring.Monitoring.finish(span)
                }

                val system = SystemSettings(
                    screensaverSelected = screensaverSelected,
                    openScreensaverSettings = { openScreensaverSettings(this@MainActivity) },
                    openDisplaySettings = { openDisplaySettings(this@MainActivity) }
                )
                val navigation = Navigation(
                    openTemplates = { page = "templates" },
                    openFocus = { page = "focus" },
                    openSetup = { page = "setup" },
                    openScreensaverTest = { page = "screensaver" },
                    openDebug = { page = "debug" },
                    backToDashboard = { page = "dashboard" }
                )

                Box(Modifier.fillMaxSize()) {
                DockMorphHost {
                AnimatedContent(
                    targetState = targetState,
                    transitionSpec = {
                        val morph = AuraDockMotion.tween<Float>(AuraDockMotion.DockMorph, AuraDockMotion.EasingEmphasized)
                        when {
                            // Opening the dock: it grows from its thumbnail size to the full screen.
                            targetState == "dock" -> (fadeIn(morph) + scaleIn(initialScale = AuraDockMotion.DockMorphScale, animationSpec = morph))
                                .togetherWith(fadeOut(animationSpec = tween(AuraDockMotion.Fast)))
                            // Closing it: the reverse, shrinking back to the thumbnail.
                            initialState == "dock" -> fadeIn(animationSpec = AuraDockMotion.tween(AuraDockMotion.DockMorph, AuraDockMotion.EasingEmphasized))
                                .togetherWith(fadeOut(morph) + scaleOut(targetScale = AuraDockMotion.DockMorphScale, animationSpec = morph))
                            else -> (fadeIn(animationSpec = tween(AuraDockMotion.ScreenIn)) + scaleIn(initialScale = 0.985f, animationSpec = tween(AuraDockMotion.ScreenIn)))
                                .togetherWith(
                                    fadeOut(animationSpec = tween(AuraDockMotion.ScreenOut)) + scaleOut(targetScale = 0.99f, animationSpec = tween(AuraDockMotion.ScreenOut))
                                )
                        }
                    },
                    label = "rootModeTransition"
                ) { mode ->
                    // On a tablet (Android 16 ignores the portrait lock there) the app screens keep a phone-sized column in the middle; the dock fills the screen.
                    PageWidth(limit = mode != "dock") {
                    when (mode) {
                        // One tap shows "Undo focus" (Focus on), two taps leave; swipes and a long press belong to the Split template.
                        "dock" -> Box(Modifier.fillMaxSize()) {
                            DockScreen(
                                dockData = dockData, window = this@MainActivity.window, interactive = true,
                                touch = DockTouch(onSingleTap = { standbyViewModel.onDockTapped() }, onDoubleTap = { standbyViewModel.onDockDoubleTapped() })
                            )
                            val undoShowing by standbyViewModel.undoFocusShowing.collectAsState()
                            UndoFocusPill(
                                visible = undoShowing,
                                onUndo = { standbyViewModel.undoFocus() },
                                modifier = Modifier.align(Alignment.TopCenter).padding(top = AuraDockSpacing.xl)
                            )
                        }

                        "onboarding" -> OnboardingRoute(standbyViewModel, system)
                        "gallery" -> TemplateGalleryRoute(standbyViewModel, navigation)
                        "setup" -> SetupRoute(standbyViewModel, system, onOpenScreensaverTest = navigation.openScreensaverTest, onOpenDebug = navigation.openDebug)
                        "debug" -> DebugRoute(standbyViewModel)
                        "screensaver" -> ScreensaverTestRoute(standbyViewModel, system)
                        "focus" -> FocusSettingsRoute(standbyViewModel, onOpenSetup = navigation.openSetup)
                        else -> DashboardRoute(standbyViewModel, navigation)
                    }
                    }
                }
                }
                // A permission is asked when its feature is first used: its card shows as a sheet over the screen (never over the dock).
                permissionPrompt?.let { permission ->
                    if (!dockVisible && !previewing) {
                        PermissionSheet(
                            permission = permission,
                            onAllow = { standbyViewModel.answerPrompt(allow = true) },
                            onSkip = { standbyViewModel.answerPrompt(allow = false) }
                        )
                    }
                }
                }
            }
        }
    }

    /**
     * Every screen except the dock is portrait (also set in the manifest, so the very first frame is
     * portrait). While the dock shows, the window follows the sensor between the two landscapes, so
     * "Dock now" fills the screen even when the phone is held upright; closing it goes back to portrait.
     * The auto-dock does not depend on the window turning (it reads the accelerometer), so going back to
     * portrait here cannot make it re-open.
     */
    @androidx.compose.runtime.Composable
    private fun DockOrientation(dockVisible: Boolean) {
        LaunchedEffect(dockVisible) {
            requestedOrientation = if (dockVisible) {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            } else {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
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
                // Draw into the camera cutout on the short edge, so the landscape dock is truly full screen.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) window.attributes = window.attributes.also { it.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES }
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(WindowInsetsCompat.Type.systemBars())
            }
            onDispose {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) window.attributes = window.attributes.also { it.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT }
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    override fun onStart() {
        super.onStart()
        screensaverTest.onAppOpened() // the screensaver appeared since the app was last open? the dashboard says so
        standbyViewModel.refreshCalendarPermission()
        standbyViewModel.refreshOverlayAccess()
        standbyViewModel.updateNotificationAccess(isNotificationAccessEnabled(this))
        standbyViewModel.refreshLocationPermission()
        standbyViewModel.refreshWeather() // the app is visible: refreshes only if the weather is 30+ minutes old
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openDockIfAsked(intent)
    }

    /** "Dock from anywhere" starts this activity with a request to go straight to the dock. */
    private fun openDockIfAsked(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_DOCK_NOW, false) == true) {
            intent.removeExtra(EXTRA_DOCK_NOW)
            sessions.noteWatcherLaunch()
            standbyViewModel.enterDock("watcher")
        }
    }

    override fun onResume() {
        super.onResume()
        screensaverSelected = isAuraDockScreensaver(this) // the user may have just picked it
        screensaverTest.refresh()
        standbyViewModel.refreshExactAlarmAccess() // the user may have just allowed it in settings
        standbyViewModel.refreshContactsAccess()
        standbyViewModel.refreshDndAccess()
        // ...or just allowed Do Not Disturb access
        standbyViewModel.refreshPostNotifications()
        standbyViewModel.refreshLocationPermission()
        standbyViewModel.refreshCalendarPermission()
        standbyViewModel.updateNotificationAccess(isNotificationAccessEnabled(this))
        standbyViewModel.noteGrantedPermissions() // for the local event log: permissions seen allowed
    }

    companion object {
        /** Set by the dock watcher when it opens AuraDock to show the dock. */
        const val EXTRA_DOCK_NOW = "com.auradock.extra.DOCK_NOW"
    }
}

/** Limits an app screen to a phone-sized column, centred, on wide windows (tablets, unfolded foldables); [limit] false leaves it full width. */
@androidx.compose.runtime.Composable
private fun PageWidth(limit: Boolean, content: @androidx.compose.runtime.Composable () -> Unit) {
    if (!limit) {
        content()
        return
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Box(Modifier.fillMaxHeight().widthIn(max = AuraDockSizes.pageMaxWidth)) { content() }
    }
}
