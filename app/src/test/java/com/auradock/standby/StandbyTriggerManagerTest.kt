package com.auradock.standby

import android.content.BroadcastReceiver
import android.content.ComponentCallbacks
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources
import android.os.BatteryManager
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * JVM tests for [StandbyTriggerManager]. The manager only talks to Android through a Context, so
 * we hand it a mock Context, capture the BroadcastReceiver / ComponentCallbacks it registers, and
 * then drive them by hand — exactly what the OS does when you plug in a charger or rotate.
 */
class StandbyTriggerManagerTest {

    private lateinit var context: Context
    private lateinit var prefs: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var batteryReceiver: BroadcastReceiver
    private lateinit var orientationCallbacks: ComponentCallbacks

    @Before
    fun setUp() {
        context = mock()
        prefs = mock()
        editor = mock()
        whenever(context.applicationContext).thenReturn(context)
        whenever(context.getSharedPreferences(any(), any())).thenReturn(prefs)
        whenever(prefs.edit()).thenReturn(editor)
        whenever(editor.putString(any(), any())).thenReturn(editor)
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    /** Builds a manager, starts it with the given initial hardware state and captures its listeners. */
    private fun startManager(
        mode: StandbyTriggerMode,
        charging: Boolean = false,
        landscape: Boolean = false
    ): StandbyTriggerManager {
        whenever(prefs.getString(any(), any())).thenReturn(mode.name)
        // Build the mocks first: creating a mock inside thenReturn(...) would interrupt stubbing.
        val stickyIntent = batteryIntent(charging)
        val initialConfig = config(landscape)
        whenever(context.registerReceiver(any<BroadcastReceiver>(), any<IntentFilter>()))
            .thenReturn(stickyIntent)
        val resources: Resources = mock()
        whenever(resources.configuration).thenReturn(initialConfig)
        whenever(context.resources).thenReturn(resources)

        val manager = StandbyTriggerManager(context)
        manager.start()

        val receiver = argumentCaptor<BroadcastReceiver>()
        verify(context).registerReceiver(receiver.capture(), any<IntentFilter>())
        batteryReceiver = receiver.firstValue
        val callbacks = argumentCaptor<ComponentCallbacks>()
        verify(context).registerComponentCallbacks(callbacks.capture())
        orientationCallbacks = callbacks.firstValue
        return manager
    }

    private fun batteryIntent(charging: Boolean): Intent {
        val intent: Intent = mock()
        whenever(intent.action).thenReturn(Intent.ACTION_BATTERY_CHANGED)
        whenever(intent.getIntExtra(eq(BatteryManager.EXTRA_PLUGGED), any()))
            .thenReturn(if (charging) BatteryManager.BATTERY_PLUGGED_AC else 0)
        return intent
    }

    private fun config(landscape: Boolean) = Configuration().apply {
        orientation = if (landscape) Configuration.ORIENTATION_LANDSCAPE else Configuration.ORIENTATION_PORTRAIT
    }

    private fun plug(charging: Boolean) = batteryReceiver.onReceive(context, batteryIntent(charging))

    private fun rotate(landscape: Boolean) = orientationCallbacks.onConfigurationChanged(config(landscape))

    private fun StandbyTriggerManager.state() = standbyState.value

    // ── trigger modes ─────────────────────────────────────────────────────────

    @Test
    fun `charging and landscape mode needs both conditions`() {
        val manager = startManager(StandbyTriggerMode.CHARGING_AND_LANDSCAPE)
        assertEquals(StandbyState.INACTIVE, manager.state())

        plug(true)  // charging only
        assertEquals(StandbyState.INACTIVE, manager.state())

        rotate(true) // charging + landscape
        assertEquals(StandbyState.ACTIVE, manager.state())
    }

    @Test
    fun `charging and landscape mode ignores landscape alone`() {
        val manager = startManager(StandbyTriggerMode.CHARGING_AND_LANDSCAPE)
        rotate(true)
        assertEquals(StandbyState.INACTIVE, manager.state())
    }

    @Test
    fun `charging only mode activates on plug regardless of orientation`() {
        val manager = startManager(StandbyTriggerMode.CHARGING_ONLY)
        rotate(false)
        plug(true)
        assertEquals(StandbyState.ACTIVE, manager.state())
    }

    @Test
    fun `landscape only mode activates on rotate regardless of charging`() {
        val manager = startManager(StandbyTriggerMode.LANDSCAPE_ONLY)
        plug(false)
        rotate(true)
        assertEquals(StandbyState.ACTIVE, manager.state())
    }

    // ── entering and exiting ──────────────────────────────────────────────────

    @Test
    fun `starts active when already charging in landscape`() {
        val manager = startManager(StandbyTriggerMode.CHARGING_AND_LANDSCAPE, charging = true, landscape = true)
        assertEquals(StandbyState.ACTIVE, manager.state())
    }

    @Test
    fun `unplugging exits standby`() {
        val manager = startManager(StandbyTriggerMode.CHARGING_AND_LANDSCAPE, charging = true, landscape = true)
        plug(false)
        assertEquals(StandbyState.INACTIVE, manager.state())
    }

    @Test
    fun `rotating back to portrait exits standby`() {
        val manager = startManager(StandbyTriggerMode.CHARGING_AND_LANDSCAPE, charging = true, landscape = true)
        rotate(false)
        assertEquals(StandbyState.INACTIVE, manager.state())
    }

    @Test
    fun `standby can be re-entered after exiting`() {
        val manager = startManager(StandbyTriggerMode.CHARGING_AND_LANDSCAPE, charging = true, landscape = true)
        rotate(false)
        rotate(true)
        assertEquals(StandbyState.ACTIVE, manager.state())
    }

    @Test
    fun `stop forces inactive and unregisters listeners`() {
        val manager = startManager(StandbyTriggerMode.CHARGING_AND_LANDSCAPE, charging = true, landscape = true)
        manager.stop()
        assertEquals(StandbyState.INACTIVE, manager.state())
        verify(context).unregisterReceiver(batteryReceiver)
        verify(context).unregisterComponentCallbacks(orientationCallbacks)
    }

    // ── mode switching ────────────────────────────────────────────────────────

    @Test
    fun `changing mode re-evaluates the current state`() {
        // charging in portrait: inactive under CHARGING_AND_LANDSCAPE, active under CHARGING_ONLY
        val manager = startManager(StandbyTriggerMode.CHARGING_AND_LANDSCAPE, charging = true, landscape = false)
        assertEquals(StandbyState.INACTIVE, manager.state())
        manager.setTriggerMode(StandbyTriggerMode.CHARGING_ONLY)
        assertEquals(StandbyState.ACTIVE, manager.state())
    }

    @Test
    fun `cycleTriggerMode walks all three modes and persists each`() {
        val manager = startManager(StandbyTriggerMode.CHARGING_AND_LANDSCAPE)
        manager.cycleTriggerMode()
        assertEquals(StandbyTriggerMode.CHARGING_ONLY, manager.triggerMode.value)
        manager.cycleTriggerMode()
        assertEquals(StandbyTriggerMode.LANDSCAPE_ONLY, manager.triggerMode.value)
        manager.cycleTriggerMode()
        assertEquals(StandbyTriggerMode.CHARGING_AND_LANDSCAPE, manager.triggerMode.value)
        verify(editor).putString(any(), eq(StandbyTriggerMode.CHARGING_ONLY.name))
        verify(editor).putString(any(), eq(StandbyTriggerMode.LANDSCAPE_ONLY.name))
    }
}
