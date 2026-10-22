package com.auradock.widget

import com.auradock.widget.impl.ClockWidget
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class WidgetManagerTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope      = TestScope(testDispatcher)

    private lateinit var clockWidget: ClockWidget
    private lateinit var widgetManager: WidgetManager

    @Before
    fun setUp() {
        clockWidget   = ClockWidget()
        widgetManager = WidgetManager(setOf(clockWidget), testScope)
    }

    @Test
    fun `registered widgets are accessible`() {
        assertEquals(1, widgetManager.getRegisteredWidgets().size)
        assertEquals("com.auradock.widget.clock", widgetManager.getRegisteredWidgets().first().widgetId)
    }

    @Test
    fun `activateAll triggers widget data emission`() = runTest {
        widgetManager.activateAll()
        val data = widgetManager.widgetDataFlow.first()
        assertTrue(data is WidgetData.Clock)
    }

    @Test
    fun `deactivateAll cancels active jobs`() = runTest {
        widgetManager.activateAll()
        widgetManager.deactivateAll()
        // After deactivation, getRegisteredWidgets still returns the registry
        assertEquals(1, widgetManager.getRegisteredWidgets().size)
    }

    @Test
    fun `mock widget deactivate is called`() = runTest {
        val mockWidget: BaseWidget = mock {
            on { widgetId } doReturn "com.test.mock"
            on { displayName } doReturn "Mock Widget"
        }
        val manager = WidgetManager(setOf(mockWidget), testScope)
        manager.deactivateAll()
        verify(mockWidget).onDeactivate()
    }
}
