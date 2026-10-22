package com.auradock.standby

import android.content.BroadcastReceiver
import android.content.ComponentCallbacks
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.BatteryManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class StandbyTriggerManager(
    private val context: Context
) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _standbyState = MutableStateFlow(StandbyState.INACTIVE)
    val standbyState: StateFlow<StandbyState> = _standbyState.asStateFlow()

    private val _triggerMode = MutableStateFlow(readModeFromPrefs())
    val triggerMode: StateFlow<StandbyTriggerMode> = _triggerMode.asStateFlow()

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

    private val orientationCallbacks = object : ComponentCallbacks {
        override fun onConfigurationChanged(newConfig: Configuration) {
            isLandscape = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
            recomputeStandbyState()
        }

        override fun onLowMemory() = Unit
    }

    fun start() {
        if (isStarted) return
        isStarted = true

        runCatching {
            val stickyIntent = appContext.registerReceiver(
                batteryReceiver,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )
            isCharging = stickyIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
            isLandscape = appContext.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

            appContext.registerComponentCallbacks(orientationCallbacks)
            recomputeStandbyState()
            Log.d(TAG, "StandbyTriggerManager started. charging=$isCharging landscape=$isLandscape mode=${_triggerMode.value}")
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

        runCatching {
            appContext.unregisterComponentCallbacks(orientationCallbacks)
        }.onFailure {
            Log.w(TAG, "ComponentCallbacks already unregistered", it)
        }

        transitionTo(StandbyState.INACTIVE)
        Log.d(TAG, "StandbyTriggerManager stopped")
    }

    fun setTriggerMode(mode: StandbyTriggerMode) {
        _triggerMode.value = mode
        prefs.edit().putString(KEY_TRIGGER_MODE, mode.name).apply()
        Log.d(TAG, "Trigger mode changed to $mode")
        recomputeStandbyState()
    }

    fun cycleTriggerMode() {
        val next = when (_triggerMode.value) {
            StandbyTriggerMode.CHARGING_AND_LANDSCAPE -> StandbyTriggerMode.CHARGING_ONLY
            StandbyTriggerMode.CHARGING_ONLY -> StandbyTriggerMode.LANDSCAPE_ONLY
            StandbyTriggerMode.LANDSCAPE_ONLY -> StandbyTriggerMode.CHARGING_AND_LANDSCAPE
        }
        setTriggerMode(next)
    }

    private fun readModeFromPrefs(): StandbyTriggerMode {
        val stored = prefs.getString(KEY_TRIGGER_MODE, StandbyTriggerMode.CHARGING_AND_LANDSCAPE.name)
        return runCatching { StandbyTriggerMode.valueOf(stored.orEmpty()) }
            .getOrDefault(StandbyTriggerMode.CHARGING_AND_LANDSCAPE)
    }

    private fun recomputeStandbyState() {
        val next = when (_triggerMode.value) {
            StandbyTriggerMode.CHARGING_AND_LANDSCAPE -> {
                if (isCharging && isLandscape) StandbyState.ACTIVE else StandbyState.INACTIVE
            }

            StandbyTriggerMode.CHARGING_ONLY -> {
                if (isCharging) StandbyState.ACTIVE else StandbyState.INACTIVE
            }

            StandbyTriggerMode.LANDSCAPE_ONLY -> {
                if (isLandscape) StandbyState.ACTIVE else StandbyState.INACTIVE
            }
        }
        transitionTo(next)
    }

    private fun transitionTo(next: StandbyState) {
        if (_standbyState.value != next) {
            Log.d(TAG, "Standby transition: ${_standbyState.value} -> $next")
            _standbyState.value = next
        }
    }

    companion object {
        private const val TAG = "StandbyTriggerManager"
        private const val PREFS_NAME = "auradock_standby_prefs"
        private const val KEY_TRIGGER_MODE = "standby_trigger_mode"
    }
}
