package com.auradock.core

import android.util.Log
import com.auradock.gesture.GestureEvent
import com.auradock.widget.WidgetManager
import com.auradock.worker.WorkManagerScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central controller — the heart of AuraDock.
 */
@Singleton
class StandbyController @Inject constructor(
    private val widgetManager: WidgetManager,
    private val scheduler: WorkManagerScheduler,
    @Suppress("UnusedPrivateMember")
    private val scope: CoroutineScope
) {
    private val _state = MutableStateFlow<StandbyState>(StandbyState.IDLE)
    val state: StateFlow<StandbyState> = _state.asStateFlow()

    // ── Power Events ──────────────────────────────────────────────────────────

    fun onChargingStarted() {
        if (_state.value == StandbyState.IDLE) {
            transition(StandbyState.CHARGING)
        }
    }

    fun onChargingStopped() {
        if (_state.value != StandbyState.IDLE) {
            transition(StandbyState.IDLE)
        }
    }

    // ── Orientation Events ─────────────────────────────────────────────────────

    fun onLandscapeDetected() {
        if (_state.value == StandbyState.CHARGING) {
            transition(StandbyState.STANDBY_ACTIVE)
        }
    }

    fun onPortraitDetected() {
        val current = _state.value
        if (current == StandbyState.STANDBY_ACTIVE || current is StandbyState.STANDBY_SLEEP) {
            transition(StandbyState.CHARGING)
        }
    }

    // ── Gesture Events ─────────────────────────────────────────────────────────

    fun onGestureReceived(event: GestureEvent) {
        if (_state.value is StandbyState.STANDBY_SLEEP) {
            Log.d(TAG, "[StandbyController] Gesture received: $event — waking up")
            transition(StandbyState.STANDBY_ACTIVE)
        }
    }

    // ── Sleep Timeout ──────────────────────────────────────────────────────────

    fun onSleepTimeout() {
        if (_state.value == StandbyState.STANDBY_ACTIVE) {
            transition(StandbyState.STANDBY_SLEEP())
        }
    }

    // ── Internal Transition ────────────────────────────────────────────────────

    private fun transition(newState: StandbyState) {
        val oldState = _state.value
        if (oldState != newState) {
            Log.d(TAG, "[StandbyController] State transition: $oldState → $newState")
            _state.value = newState
            applySideEffects(newState)
        }
    }

    private fun applySideEffects(to: StandbyState) {
        when (to) {
            is StandbyState.STANDBY_ACTIVE -> {
                Log.d(TAG, "[StandbyController] Standby mode activated")
                widgetManager.activateAll()
                scheduler.scheduleWidgetUpdates()
            }
            is StandbyState.STANDBY_SLEEP -> {
                widgetManager.suspendAll()
            }
            is StandbyState.IDLE, is StandbyState.CHARGING -> {
                if (to == StandbyState.IDLE) Log.d(TAG, "[StandbyController] System IDLE")
                widgetManager.deactivateAll()
                scheduler.cancelWidgetUpdates()
            }
        }
    }

    companion object {
        private const val TAG = "AuraDock"
    }
}
