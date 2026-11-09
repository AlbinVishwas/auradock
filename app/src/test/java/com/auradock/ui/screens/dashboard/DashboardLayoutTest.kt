package com.auradock.ui.screens.dashboard

import com.auradock.ui.viewmodel.CalendarPermissionUiState.DENIED
import com.auradock.ui.viewmodel.CalendarPermissionUiState.GRANTED
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardLayoutTest {
    @Test fun `an empty day is nothing planned with the calendar allowed and no events`() {
        assertTrue(isEmptyDay(tasksEmpty = true, hasIntention = false, calendar = GRANTED, eventsEmpty = true))
    }

    @Test fun `anything on the day, or still loading, is the fuller layout`() {
        assertFalse(isEmptyDay(false, false, GRANTED, true))
        assertFalse(isEmptyDay(true, true, GRANTED, true))
        assertFalse(isEmptyDay(true, false, GRANTED, false))
        assertFalse(isEmptyDay(null, false, GRANTED, true))
        assertFalse(isEmptyDay(true, false, GRANTED, null))
        assertFalse(isEmptyDay(true, false, DENIED, true))
    }

    @Test fun `the Focus tile says whether it is on`() {
        assertEquals("On while docked", focusTileText(true))
        assertEquals("Off", focusTileText(false))
    }
}
