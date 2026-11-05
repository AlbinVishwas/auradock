package com.auradock.standby

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.auradock.dock.PhysicalOrientation
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
 * then drive them by hand, exactly what the OS does when you plug in a charger or rotate.
 * The rule under test: dock when charging AND landscape.
 */
class StandbyTriggerManagerTest {

    private lateinit var context: Context
    private lateinit var batteryReceiver: BroadcastReceiver

    @Before
    fun setUp() {
        context = mock()
        whenever(context.applicationContext).thenReturn(context)
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun startManager(charging: Boolean = false, landscape: Boolean = false): StandbyTriggerManager {
        // Build the mocks first: creating a mock inside thenReturn(...) would interrupt stubbing.
        val stickyIntent = batteryIntent(charging)
        whenever(context.registerReceiver(any<BroadcastReceiver>(), any<IntentFilter>())).thenReturn(stickyIntent)
        return startAndCapture().also { if (landscape) it.rotate(true) }
    }

    private fun startAndCapture(): StandbyTriggerManager {
        val manager = StandbyTriggerManager(context)
        manager.start()
        val receiver = argumentCaptor<BroadcastReceiver>()
        verify(context).registerReceiver(receiver.capture(), any<IntentFilter>())
        batteryReceiver = receiver.firstValue
        return manager
    }

    private fun batteryIntent(charging: Boolean): Intent {
        val intent: Intent = mock()
        whenever(intent.action).thenReturn(Intent.ACTION_BATTERY_CHANGED)
        whenever(intent.getIntExtra(eq(BatteryManager.EXTRA_PLUGGED), any()))
            .thenReturn(if (charging) BatteryManager.BATTERY_PLUGGED_AC else 0)
        return intent
    }

    private fun plug(charging: Boolean) = batteryReceiver.onReceive(context, batteryIntent(charging))
    // The accelerometer is out of reach of a JVM test, so tests hand the manager the steady reading directly.
    private fun StandbyTriggerManager.rotate(landscape: Boolean) =
        onStableOrientation(if (landscape) PhysicalOrientation.LANDSCAPE_LEFT else PhysicalOrientation.PORTRAIT)
    private fun StandbyTriggerManager.state() = standbyState.value

    // ── the rule: charging AND landscape ──────────────────────────────────────

    @Test
    fun `needs both charging and landscape`() {
        val manager = startManager()
        assertEquals(StandbyState.INACTIVE, manager.state())
        plug(true) // charging only
        assertEquals(StandbyState.INACTIVE, manager.state())
        manager.rotate(true) // charging + landscape
        assertEquals(StandbyState.ACTIVE, manager.state())
    }

    @Test
    fun `landscape alone does not trigger`() {
        val manager = startManager()
        manager.rotate(true)
        assertEquals(StandbyState.INACTIVE, manager.state())
    }

    @Test
    fun `starts active when already charging in landscape`() {
        assertEquals(StandbyState.ACTIVE, startManager(charging = true, landscape = true).state())
    }

    // ── entering and exiting ──────────────────────────────────────────────────

    @Test
    fun `unplugging exits standby`() {
        val manager = startManager(charging = true, landscape = true)
        plug(false)
        assertEquals(StandbyState.INACTIVE, manager.state())
    }

    @Test
    fun `rotating back to portrait exits standby`() {
        val manager = startManager(charging = true, landscape = true)
        manager.rotate(false)
        assertEquals(StandbyState.INACTIVE, manager.state())
    }

    @Test
    fun `standby can be re-entered after exiting`() {
        val manager = startManager(charging = true, landscape = true)
        manager.rotate(false)
        manager.rotate(true)
        assertEquals(StandbyState.ACTIVE, manager.state())
    }

    @Test
    fun `stop forces inactive and unregisters listeners`() {
        val manager = startManager(charging = true, landscape = true)
        manager.stop()
        assertEquals(StandbyState.INACTIVE, manager.state())
        verify(context).unregisterReceiver(batteryReceiver)
    }

    // ── missing battery info ──────────────────────────────────────────────────

    @Test
    fun `missing sticky battery intent is treated as not charging`() {
        // registerReceiver returns null when no ACTION_BATTERY_CHANGED broadcast has been sent yet.
        // The phone is in landscape, so a wrongly assumed "charging" would turn standby on.
        whenever(context.registerReceiver(any<BroadcastReceiver>(), any<IntentFilter>())).thenReturn(null)

        val manager = startAndCapture()
        manager.rotate(true)
        assertEquals(StandbyState.INACTIVE, manager.state())
    }
}
