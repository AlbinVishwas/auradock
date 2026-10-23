package com.auradock.dock

import android.service.dreams.DreamService
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.auradock.ui.screens.StandbyScreen
import com.auradock.ui.template.TemplateDefaults
import com.auradock.ui.template.sanitizedForStandby
import com.auradock.ui.theme.AuraDockTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * AuraDock as an Android screensaver ("Daydream").
 *
 * The system starts this service when the phone is charging and the screen timeout passes, and stops it
 * when the user touches the screen. We show the same standby screen the app shows.
 *
 * ## Why the lifecycle boilerplate
 * Compose draws inside a View, and before it will run it looks up the view tree for two helpers:
 *  - a **LifecycleOwner**, so it knows when it may animate and recompose (it pauses when the lifecycle
 *    is not at least STARTED, so it does not burn battery in the background);
 *  - a **SavedStateRegistryOwner**, where it keeps `rememberSaveable` state across recreation.
 * `ComponentActivity` provides both for free. A `DreamService` is a plain Service, so it provides neither,
 * and without them Compose throws "ViewTreeLifecycleOwner not found". So this class *is* the owner
 * of both and attaches itself to the Compose view, and it moves a small lifecycle through
 * CREATED -> STARTED/RESUMED -> CREATED -> DESTROYED in step with the dream's own callbacks.
 */
@AndroidEntryPoint
class DockDreamService : DreamService(), LifecycleOwner, SavedStateRegistryOwner {

    @Inject lateinit var dockData: DockData

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    override fun onCreate() {
        super.onCreate() // Hilt injects dockData here
        savedStateController.performAttach()
        savedStateController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isInteractive = false // a touch ends the dream
        isFullscreen = true   // hide the status bar

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@DockDreamService)
            setViewTreeSavedStateRegistryOwner(this@DockDreamService)
            setContent {
                AuraDockTheme {
                    val time by dockData.timeMillis.collectAsState(initial = dockData.currentTimeMillis())
                    val events by dockData.todayEvents.collectAsState(initial = emptyList())
                    val notifications by dockData.notifications.collectAsState()
                    StandbyScreen(
                        template = TemplateDefaults.Minimal.sanitizedForStandby(),
                        timeMillis = time,
                        todayEvents = events,
                        calendarPermissionState = dockData.calendarPermissionState(),
                        importantNotifications = notifications,
                        hasNotificationAccess = dockData.hasNotificationAccess()
                    )
                }
            }
        }
        setContentView(composeView)
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    override fun onDreamingStopped() {
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        super.onDreamingStopped()
    }

    override fun onDetachedFromWindow() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        super.onDetachedFromWindow()
    }
}
