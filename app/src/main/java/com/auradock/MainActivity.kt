package com.auradock

import android.Manifest
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.viewModels
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.auradock.permissions.isNotificationAccessEnabled
import com.auradock.permissions.openAppSettings
import com.auradock.permissions.openNotificationAccessSettings
import com.auradock.standby.StandbyState
import com.auradock.ui.screens.StandbyScreen
import com.auradock.ui.screens.TemplateGalleryScreen
import com.auradock.ui.screens.dashboard.DashboardScreen
import com.auradock.ui.theme.AuraDockTheme
import com.auradock.ui.viewmodel.PermissionAction
import com.auradock.ui.viewmodel.StandbyViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val standbyViewModel: StandbyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep the screen on only while standby is active and this Activity is visible.
        // Collected here in onCreate (the supported place for repeatOnLifecycle). The flag lives on
        // the window, which is recreated on rotation, so we re-apply it from the ViewModel's state.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                standbyViewModel.standbyState.collect { state ->
                    if (state == StandbyState.ACTIVE) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }
            }
        }
        setContent {
            AuraDockTheme {
                val currentTemplate by standbyViewModel.currentTemplate.collectAsState()
                val timeMillis by standbyViewModel.currentTimeMillis.collectAsState()
                val standbyState by standbyViewModel.standbyState.collectAsState()
                val todayEvents by standbyViewModel.todayEvents.collectAsState()
                val calendarPermissionState by standbyViewModel.calendarPermissionState.collectAsState()
                val importantNotifications by standbyViewModel.importantNotifications.collectAsState()
                val hasNotificationAccess by standbyViewModel.hasNotificationAccess.collectAsState()
                var showGallery by remember { mutableStateOf(false) }
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

                val targetState = when {
                    standbyState == StandbyState.ACTIVE -> "standby"
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
                        "standby" -> StandbyScreen(
                            template = currentTemplate,
                            timeMillis = timeMillis,
                            todayEvents = todayEvents,
                            calendarPermissionState = calendarPermissionState,
                            importantNotifications = importantNotifications,
                            hasNotificationAccess = hasNotificationAccess,
                            onEnableCalendarAccess = { standbyViewModel.requestCalendarPermission() },
                            onOpenNotificationSettings = { standbyViewModel.openNotificationSettings() }
                        )

                        "gallery" -> TemplateGalleryScreen(
                            onPreviewClick = { standbyViewModel.setTemplate(it) },
                            onApplyClick = {
                                standbyViewModel.setTemplate(it)
                                showGallery = false
                            }
                        )

                        else -> DashboardScreen(
                            currentTemplate = currentTemplate,
                            timeMillis = timeMillis,
                            todayEvents = todayEvents,
                            calendarPermissionState = calendarPermissionState,
                            importantNotifications = importantNotifications,
                            hasNotificationAccess = hasNotificationAccess,
                            onCustomizeStandby = { showGallery = true },
                            onEnableCalendarAccess = { standbyViewModel.requestCalendarPermission() },
                            onOpenNotificationSettings = { standbyViewModel.openNotificationSettings() }
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        standbyViewModel.refreshCalendarPermission()
        standbyViewModel.updateNotificationAccess(isNotificationAccessEnabled(this))
    }

    override fun onStop() {
        super.onStop()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onResume() {
        super.onResume()
        standbyViewModel.refreshCalendarPermission()
        standbyViewModel.updateNotificationAccess(isNotificationAccessEnabled(this))
    }
}
