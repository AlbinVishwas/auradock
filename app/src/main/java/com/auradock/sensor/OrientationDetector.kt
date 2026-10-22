package com.auradock.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import com.auradock.core.StandbyController
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.sample
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * Wraps [SensorManager] to detect dock (landscape) vs portrait orientation.
 */
@Singleton
class OrientationDetector @Inject constructor(
    @ApplicationContext private val context: Context,
    private val controller: StandbyController
) {
    enum class OrientationEvent { LANDSCAPE, PORTRAIT, FLAT }

    private val TAG = "AuraDock"
    private val LANDSCAPE_THRESHOLD = 7.0f
    private val DEBOUNCE_MS = 600L // 600ms debounce window

    @OptIn(FlowPreview::class)
    val orientationFlow: Flow<OrientationEvent> = callbackFlow {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            ?: run {
                Log.w(TAG, "[OrientationDetector] No accelerometer found — orientation detection disabled")
                close()
                return@callbackFlow
            }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val x = event.values[0]
                val z = event.values[2]
                
                // Determine orientation based on dominant axis
                val orientation = when {
                    abs(x) >= LANDSCAPE_THRESHOLD -> OrientationEvent.LANDSCAPE
                    abs(z) >= LANDSCAPE_THRESHOLD -> OrientationEvent.FLAT
                    else                           -> OrientationEvent.PORTRAIT
                }
                
                // We do NOT log raw values or every event here to prevent spam.
                // The flow pipeline below handles filtering and debouncing.
                trySend(orientation)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }
    .distinctUntilChanged() // Pre-filter identical consecutive events
    .sample(DEBOUNCE_MS)    // Wait for stability
    .distinctUntilChanged() // Only emit if the stable state is different from the last emitted state

    /**
     * Collects [orientationFlow] and translates events into [StandbyController] calls.
     */
    suspend fun startObserving() {
        Log.d(TAG, "[OrientationDetector] Monitoring orientation changes (debounce: ${DEBOUNCE_MS}ms)")
        
        var lastOrientation: OrientationEvent? = null
        
        orientationFlow.collect { event ->
            if (lastOrientation != null && lastOrientation != event) {
                Log.d(TAG, "[OrientationDetector] Orientation Transition: $lastOrientation → $event")
            } else if (lastOrientation == null) {
                Log.d(TAG, "[OrientationDetector] Initial Orientation: $event")
            }
            
            lastOrientation = event
            
            when (event) {
                OrientationEvent.LANDSCAPE -> controller.onLandscapeDetected()
                OrientationEvent.PORTRAIT,
                OrientationEvent.FLAT -> controller.onPortraitDetected()
            }
        }
    }
}
