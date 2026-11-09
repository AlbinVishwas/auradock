package com.auradock.dock

import com.auradock.ui.theme.AuraDockColors
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
import android.os.SystemClock
import android.service.dreams.DreamService
import android.util.Log
import android.view.Display
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import com.auradock.ui.theme.AuraDockTheme
import com.auradock.focus.DockFocusController
import com.auradock.focus.DockSource
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
    @Inject lateinit var dockFocus: DockFocusController
    @Inject lateinit var screensaverTest: com.auradock.screensaver.ScreensaverTest

    private val handler = Handler(Looper.getMainLooper())
    private val stabilizer = OrientationStabilizer()
    private val gate = DockOrientationGate()

    /** Null while the screen should stay black. Read by Compose, so changing it redraws. */
    private var shownOrientation by mutableStateOf<PhysicalOrientation?>(null)
    private var displayRotationDegrees by mutableIntStateOf(0)

    private var trace = GateTrace(SystemClock.elapsedRealtime())
    private var lastStable: PhysicalOrientation? = null
    private var sensorSlowed = false
    private var finishing = false

    private val firstReadingTimeout = Runnable {
        Log.d(TAG, trace.describe(SystemClock.elapsedRealtime(), "timeout", lastStable))
        apply(gate.onTimeout())
    }

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (finishing) return // finish() is asynchronous; readings keep arriving until the service stops
            val (x, y, z) = event.values
            val raw = classifyOrientation(x, y, z)
            trace.onSample(SystemClock.elapsedRealtime(), x, y, z, raw)
            stabilizer.onSample(raw)?.let {
                lastStable = it
                apply(gate.onStableOrientation(it))
            }
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
                    val shown = shownOrientation
                    if (shown == null) {
                        Box(Modifier.fillMaxSize().background(AuraDockColors.Background)) // waiting for the sensor
                    } else {
                        RotatedContent(contentRotationDegrees(shown, displayRotationDegrees)) {
                            DockScreen(dockData = dockData, window = window)
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
        trace = GateTrace(SystemClock.elapsedRealtime())
        finishing = false
        val registered = sensorManager.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        Log.d(TAG, "gate started: accelerometer registered=$registered")
        handler.postDelayed(firstReadingTimeout, FIRST_READING_TIMEOUT_MS)
    }

    private fun apply(decision: GateDecision) {
        when (decision) {
            GateDecision.Wait -> Unit
            GateDecision.Finish -> {
                if (finishing) return
                finishing = true
                Log.d(TAG, trace.describe(SystemClock.elapsedRealtime(), "finish (not landscape)", lastStable))
                finish()
            }
            is GateDecision.Show -> {
                handler.removeCallbacks(firstReadingTimeout)
                slowDownSensor()
                displayRotationDegrees = currentDisplayRotationDegrees()
                if (shownOrientation != decision.orientation) {
                    Log.d(
                        TAG,
                        trace.describe(SystemClock.elapsedRealtime(), "show ${decision.orientation}", lastStable) +
                            "; window rotation $displayRotationDegrees, " +
                            "content turned ${contentRotationDegrees(decision.orientation, displayRotationDegrees)}"
                    )
                }
                shownOrientation = decision.orientation
                dockFocus.setActive(DockSource.DREAM, true) // the dock is really showing: Do Not Disturb on
                screensaverTest.onDreamShown() // the guided test: "AuraDock appeared on its own"
            }
        }
    }

    /** The decision is made; only the occasional left/right switch matters from here, so read the sensor less often. */
    private fun slowDownSensor() {
        if (sensorSlowed) return
        sensorSlowed = true
        val sensorManager = getSystemService(SensorManager::class.java) ?: return
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return
        sensorManager.unregisterListener(sensorListener)
        sensorManager.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_UI)
    }

    /** How far the system has rotated the window: 0, 90, 180 or 270 (Surface.ROTATION_x times 90). */
    private fun currentDisplayRotationDegrees(): Int =
        (getSystemService(DisplayManager::class.java).getDisplay(Display.DEFAULT_DISPLAY)?.rotation ?: 0) * 90

    override fun onDreamingStopped() {
        dockFocus.setActive(DockSource.DREAM, false) // the screensaver ended: put the user's settings back
        getSystemService(SensorManager::class.java)?.unregisterListener(sensorListener)
        handler.removeCallbacks(firstReadingTimeout)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        super.onDreamingStopped()
    }

    override fun onDetachedFromWindow() {
        dockFocus.setActive(DockSource.DREAM, false)
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        super.onDetachedFromWindow()
    }

    private companion object {
        const val TAG = "DockDream"
        const val FIRST_READING_TIMEOUT_MS = 2000L
    }
}
