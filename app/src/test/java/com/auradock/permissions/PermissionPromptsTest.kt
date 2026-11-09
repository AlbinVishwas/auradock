package com.auradock.permissions

import com.auradock.ui.onboarding.SetupPermission.CALENDAR
import com.auradock.ui.onboarding.SetupPermission.DND_ACCESS
import com.auradock.ui.onboarding.SetupPermission.EXACT_ALARMS
import com.auradock.ui.onboarding.SetupPermission.POST_NOTIFICATIONS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionPromptsTest {
    @Test fun `a permission is asked on first use only`() {
        assertTrue(PermissionPrompts.askOnFirstUse(CALENDAR, granted = false, alreadyAsked = emptySet()))
        assertFalse(PermissionPrompts.askOnFirstUse(CALENDAR, granted = false, alreadyAsked = setOf("CALENDAR")))
    }

    @Test fun `an allowed permission is never asked`() {
        assertFalse(PermissionPrompts.askOnFirstUse(CALENDAR, granted = true, alreadyAsked = emptySet()))
    }

    @Test fun `a first timed task asks for notifications, then alarms`() {
        assertEquals(listOf(POST_NOTIFICATIONS, EXACT_ALARMS), PermissionPrompts.forTimedTask(emptySet(), emptySet()))
        assertEquals(listOf(EXACT_ALARMS), PermissionPrompts.forTimedTask(setOf(POST_NOTIFICATIONS), emptySet()))
        assertEquals(emptyList<Any>(), PermissionPrompts.forTimedTask(emptySet(), setOf("POST_NOTIFICATIONS", "EXACT_ALARMS")))
    }

    @Test fun `the first dock asks for Do Not Disturb only when Focus is on`() {
        assertEquals(listOf(DND_ACCESS), PermissionPrompts.forFirstDock(true, emptySet(), emptySet()))
        assertEquals(emptyList<Any>(), PermissionPrompts.forFirstDock(false, emptySet(), emptySet()))
        assertEquals(emptyList<Any>(), PermissionPrompts.forFirstDock(true, setOf(DND_ACCESS), emptySet()))
        assertEquals(emptyList<Any>(), PermissionPrompts.forFirstDock(true, emptySet(), setOf("DND_ACCESS")))
    }
}
