package com.auradock.dock

import android.content.res.Configuration
import android.graphics.Color as AndroidColor
import android.graphics.drawable.ColorDrawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.service.dreams.DreamService
import android.util.Log
import android.view.Display
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
 * ## Orientation gate
 * The screen stays black until the accelerometer gives a stable reading ([OrientationGate]). Landscape shows
 * the dock, turned to be upright whatever the window is doing ([RotatedContent]); portrait, flat, or no
 * stable reading within [FIRST_READING_TIMEOUT_MS] calls finish() so the phone sleeps normally.
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

    private val handler = Handler(Looper.getMainLooper())
    private val stabilizer = OrientationStabilizer()
    private val gate = DockOrientationGate()

    /** Null while the screen should stay black. Read by Compose, so changing it redraws. */
    private var shownOrientation by mutableStateOf<PhysicalOrientation?>(null)
    private var displayRotationDegrees by mutableIntStateOf(0)

    private val firstReadingTimeout = Runnable {
        Log.d(TAG, "no stable reading within ${FIRST_READING_TIMEOUT_MS}ms")
        apply(gate.onTimeout())
    }

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val raw = classifyOrientation(event.values[0], event.values[1], event.values[2])
            stabilizer.onSample(raw)?.let { apply(gate.onStableOrientation(it)) }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

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
        window.setBackgroundDrawable(ColorDrawable(AndroidColor.BLACK))
        displayRotationDegrees = currentDisplayRotationDegrees()

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@DockDreamService)
            setViewTreeSavedStateRegistryOwner(this@DockDreamService)
            setContent {
                AuraDockTheme {
                    val time by dockData.timeMillis.collectAsState(initial = dockData.currentTimeMillis())
                    val events by dockData.todayEvents.collectAsState(initial = emptyList())
                    val notifications by dockData.notifications.collectAsState()
                    val shown = shownOrientation
                    if (shown == null) {
                        Box(Modifier.fillMaxSize().background(Color.Black)) // waiting for the sensor
                    } else {
                        RotatedContent(contentRotationDegrees(shown, displayRotationDegrees)) {
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
            }
        }
        setContentView(composeView)
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        startOrientationGate()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        displayRotationDegrees = currentDisplayRotationDegrees() // the system rotated the window
    }

    private fun startOrientationGate() {
        val sensorManager = getSystemService(SensorManager::class.java)
        val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (accelerometer == null) {
            Log.d(TAG, "no accelerometer, finishing")
            finish()
            return
        }
        sensorManager.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        handler.postDelayed(firstReadingTimeout, FIRST_READING_TIMEOUT_MS)
    }

    private fun apply(decision: GateDecision) {
        when (decision) {
            GateDecision.Wait -> Unit
            GateDecision.Finish -> {
                Log.d(TAG, "finish: not docked in landscape")
                finish()
            }
            is GateDecision.Show -> {
                handler.removeCallbacks(firstReadingTimeout)
                displayRotationDegrees = currentDisplayRotationDegrees()
                if (shownOrientation != decision.orientation) {
                    Log.d(
                        TAG,
                        "show: ${decision.orientation}, window rotation $displayRotationDegrees, " +
                            "content turned ${contentRotationDegrees(decision.orientation, displayRotationDegrees)}"
                    )
                }
                shownOrientation = decision.orientation
            }
        }
    }

    /** How far the system has rotated the window: 0, 90, 180 or 270 (Surface.ROTATION_x times 90). */
    private fun currentDisplayRotationDegrees(): Int =
        (getSystemService(DisplayManager::class.java).getDisplay(Display.DEFAULT_DISPLAY)?.rotation ?: 0) * 90

    override fun onDreamingStopped() {
        getSystemService(SensorManager::class.java)?.unregisterListener(sensorListener)
        handler.removeCallbacks(firstReadingTimeout)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        super.onDreamingStopped()
    }

    override fun onDetachedFromWindow() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        super.onDetachedFromWindow()
    }

    private companion object {
        const val TAG = "DockDream"
        const val FIRST_READING_TIMEOUT_MS = 1000L
    }
}
