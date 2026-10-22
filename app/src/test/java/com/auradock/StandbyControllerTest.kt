package com.auradock.core

import com.auradock.gesture.GestureEvent
import com.auradock.widget.WidgetManager
import com.auradock.worker.WorkManagerScheduler
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class StandbyControllerTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope      = TestScope(testDispatcher)

    private val widgetManager: WidgetManager       = mock()
    private val scheduler: WorkManagerScheduler    = mock()

    private lateinit var controller: StandbyController

    @Before
    fun setUp() {
        controller = StandbyController(widgetManager, scheduler, testScope)
    }

    @Test
    fun `initial state is IDLE`() {
        assertEquals(StandbyState.IDLE, controller.state.value)
    }

    @Test
    fun `charging started from IDLE transitions to CHARGING`() {
        controller.onChargingStarted()
        assertEquals(StandbyState.CHARGING, controller.state.value)
    }

    @Test
    fun `landscape detected from CHARGING transitions to STANDBY_ACTIVE`() {
        controller.onChargingStarted()
        controller.onLandscapeDetected()
        assertEquals(StandbyState.STANDBY_ACTIVE, controller.state.value)
    }

    @Test
    fun `STANDBY_ACTIVE activates widgets and schedules worker`() {
        controller.onChargingStarted()
        controller.onLandscapeDetected()
        verify(widgetManager).activateAll()
        verify(scheduler).scheduleWidgetUpdates()
    }

    @Test
    fun `charging stopped from STANDBY_ACTIVE transitions to IDLE`() {
        controller.onChargingStarted()
        controller.onLandscapeDetected()
        controller.onChargingStopped()
        assertEquals(StandbyState.IDLE, controller.state.value)
    }

    @Test
    fun `sleep timeout transitions STANDBY_ACTIVE to STANDBY_SLEEP`() {
        controller.onChargingStarted()
        controller.onLandscapeDetected()
        controller.onSleepTimeout()
        assertTrue(controller.state.value is StandbyState.STANDBY_SLEEP)
    }

    @Test
    fun `gesture wakes STANDBY_SLEEP to STANDBY_ACTIVE`() {
        controller.onChargingStarted()
        controller.onLandscapeDetected()
        controller.onSleepTimeout()
        controller.onGestureReceived(GestureEvent.Tap)
        assertEquals(StandbyState.STANDBY_ACTIVE, controller.state.value)
    }

    @Test
    fun `portrait detected from STANDBY_ACTIVE transitions to CHARGING`() {
        controller.onChargingStarted()
        controller.onLandscapeDetected()
        controller.onPortraitDetected()
        assertEquals(StandbyState.CHARGING, controller.state.value)
    }

    @Test
    fun `landscape detected has no effect from IDLE`() {
        controller.onLandscapeDetected()
        assertEquals(StandbyState.IDLE, controller.state.value)
    }
}
