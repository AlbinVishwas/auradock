package com.auradock.standby

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.BatteryManager
import android.util.Log
import com.auradock.dock.OrientationStabilizer
import com.auradock.dock.PhysicalOrientation
import com.auradock.dock.classifyOrientation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Decides when the in-app dock should show: the phone is charging AND held in landscape. (One fixed rule,
 * as the V1 spec says; there are no other trigger modes.)
 *
 * "Held in landscape" comes from the accelerometer, not from the window: the app's screens are locked to
 * portrait, so the window never turns by itself. The accelerometer is only read while charging.
 */
class StandbyTriggerManager(
    context: Context
) {
    private val appContext = context.applicationContext

    private val _standbyState = MutableStateFlow(StandbyState.INACTIVE)
    val standbyState: StateFlow<StandbyState> = _standbyState.asStateFlow()

    private var isStarted = false
    private var isCharging = false
    private var isLandscape = false

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            runCatching {
                if (intent?.action != Intent.ACTION_BATTERY_CHANGED) return
                isCharging = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
                recomputeStandbyState()
            }.onFailure {
                Log.e(TAG, "Battery receiver error", it)
                transitionTo(StandbyState.INACTIVE)
            }
        }
    }

    private val stabilizer = OrientationStabilizer()
    private var sensorRegistered = false

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val raw = classifyOrientation(event.values[0], event.values[1], event.values[2])
            stabilizer.onSample(raw)?.let { onStableOrientation(it) }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    /** A steady reading of how the phone is held. Flat or portrait both mean "not landscape". */
    internal fun onStableOrientation(orientation: PhysicalOrientation) {
        isLandscape = orientation.isLandscape
        recomputeStandbyState()
    }

    fun start() {
        if (isStarted) return
        isStarted = true

        runCatching {
            val stickyIntent = appContext.registerReceiver(
                batteryReceiver,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )
            // A null sticky intent means "no battery info yet", which must read as not charging.
            // (null != 0 is true in Kotlin, so the plain comparison would wrongly say "charging".)
            isCharging = (stickyIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0) != 0
            recomputeStandbyState()
            Log.d(TAG, "StandbyTriggerManager started. charging=$isCharging landscape=$isLandscape")
        }.onFailure {
            Log.e(TAG, "Failed to start trigger manager", it)
            transitionTo(StandbyState.INACTIVE)
        }
    }

    fun stop() {
        if (!isStarted) return
        isStarted = false

        runCatching {
            appContext.unregisterReceiver(batteryReceiver)
        }.onFailure {
            Log.w(TAG, "Receiver already unregistered", it)
        }

        updateSensor(listen = false)

        transitionTo(StandbyState.INACTIVE)
        Log.d(TAG, "StandbyTriggerManager stopped")
    }

    private fun recomputeStandbyState() {
        updateSensor(listen = isStarted && isCharging)
        transitionTo(if (isCharging && isLandscape) StandbyState.ACTIVE else StandbyState.INACTIVE)
    }

    private fun updateSensor(listen: Boolean) {
        if (listen == sensorRegistered) return
        val sensorManager = appContext.getSystemService(SensorManager::class.java) ?: return
        if (listen) {
            val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return
            sensorManager.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_UI)
            sensorRegistered = true
        } else {
            sensorManager.unregisterListener(sensorListener)
            sensorRegistered = false
            isLandscape = false // unknown until the sensor reports again
        }
    }

    private fun transitionTo(next: StandbyState) {
        if (_standbyState.value != next) {
            Log.d(TAG, "Standby transition: ${_standbyState.value} -> $next")
            _standbyState.value = next
        }
    }

    companion object {
        private const val TAG = "StandbyTriggerManager"
    }
}
