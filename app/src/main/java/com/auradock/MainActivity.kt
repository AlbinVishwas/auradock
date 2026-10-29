package com.auradock

import android.Manifest
import android.content.pm.ActivityInfo
import android.content.res.Configuration
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
import com.auradock.permissions.isNotificationAccessEnabled
import com.auradock.permissions.openAppSettings
import com.auradock.permissions.openNotificationAccessSettings
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AuraDockTheme {
                val currentTemplate by standbyViewModel.currentTemplate.collectAsState()
                val timeMillis by standbyViewModel.currentTimeMillis.collectAsState()
                val dockVisible by standbyViewModel.dockVisible.collectAsState()
                val manualDock by standbyViewModel.manualDock.collectAsState()
                val todayEvents by standbyViewModel.todayEvents.collectAsState()
                val tasks by standbyViewModel.tasks.collectAsState()
                val calendarPermissionState by standbyViewModel.calendarPermissionState.collectAsState()
                var showGallery by rememberSaveable { mutableStateOf(false) }
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
                        }
                    }
                }

                ManualDockOrientation(manualDock)
                DockWindowMode(dockVisible)
                BackHandler(enabled = dockVisible) { standbyViewModel.dismissDock() }
                BackHandler(enabled = showGallery && !dockVisible) { showGallery = false }

                val targetState = when {
                    dockVisible -> "dock"
                    showGallery -> "gallery"
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

                        "gallery" -> TemplateGalleryScreen(
                            selected = currentTemplate,
                            timeMillis = timeMillis,
                            onApplyClick = {
                                standbyViewModel.setTemplate(it)
                                showGallery = false
                            }
                        )

                        else -> DashboardScreen(
                            currentTemplate = currentTemplate,
                            timeMillis = timeMillis,
                            todayEvents = todayEvents,
                            tasks = tasks,
                            onSaveTask = { id, title, dueAt -> standbyViewModel.saveTask(id, title, dueAt) },
                            onToggleTask = { standbyViewModel.setTaskDone(it, !it.done) },
                            onDeleteTask = { standbyViewModel.deleteTask(it) },
                            calendarPermissionState = calendarPermissionState,
                            onOpenTemplates = { showGallery = true },
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
    }

    override fun onResume() {
        super.onResume()
        standbyViewModel.refreshCalendarPermission()
        standbyViewModel.updateNotificationAccess(isNotificationAccessEnabled(this))
    }
}
