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

/**
 * Decides when the in-app dock should show: the phone is charging AND in landscape. (One fixed rule,
 * as the V1 spec says; there are no other trigger modes.)
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
            // A null sticky intent means "no battery info yet", which must read as not charging.
            // (null != 0 is true in Kotlin, so the plain comparison would wrongly say "charging".)
            isCharging = (stickyIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0) != 0
            isLandscape = appContext.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

            appContext.registerComponentCallbacks(orientationCallbacks)
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

        runCatching {
            appContext.unregisterComponentCallbacks(orientationCallbacks)
        }.onFailure {
            Log.w(TAG, "ComponentCallbacks already unregistered", it)
        }

        transitionTo(StandbyState.INACTIVE)
        Log.d(TAG, "StandbyTriggerManager stopped")
    }

    private fun recomputeStandbyState() {
        transitionTo(if (isCharging && isLandscape) StandbyState.ACTIVE else StandbyState.INACTIVE)
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
