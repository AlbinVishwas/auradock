package com.auradock.screensaver

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreensaverTestRulesTest {
    private val day = SCREENSAVER_HINT_AFTER_MS
    private fun banner(dreamAt: Long = 0, verifiedAt: Long = 0, dismissed: Boolean = false, installedAt: Long = 0, now: Long = 0, snoozed: Long = 0) =
        screensaverBanner(dreamAt, verifiedAt, dismissed, installedAt, now, snoozed)

    @Test fun `nothing is said in the first day`() {
        assertEquals(ScreensaverBanner.NONE, banner(installedAt = 0, now = day - 1))
    }

    @Test fun `a day after install with no screensaver there is a gentle hint`() {
        assertEquals(ScreensaverBanner.HINT, banner(installedAt = 0, now = day))
    }

    @Test fun `trying again snoozes the hint for a day`() {
        assertEquals(ScreensaverBanner.NONE, banner(installedAt = 0, now = day + 5, snoozed = day + 1000))
        assertEquals(ScreensaverBanner.HINT, banner(installedAt = 0, now = day + 2000, snoozed = day + 1000))
    }

    @Test fun `once the dream appeared the note comes on the next app open, then until dismissed`() {
        assertEquals(ScreensaverBanner.NONE, banner(dreamAt = 10, now = day * 3))
        assertEquals(ScreensaverBanner.APPEARED, banner(dreamAt = 10, verifiedAt = 20, now = day * 3))
        assertEquals(ScreensaverBanner.NONE, banner(dreamAt = 10, verifiedAt = 20, dismissed = true, now = day * 3))
    }

    @Test fun `no hint once the screensaver has worked`() {
        assertEquals(ScreensaverBanner.NONE, banner(dreamAt = 10, verifiedAt = 20, dismissed = true, installedAt = 0, now = day * 9))
    }

    @Test fun `a screen timeout up to five minutes is fine`() {
        assertTrue(timeoutOk(30_000))
        assertTrue(timeoutOk(5L * 60 * 1000))
        assertFalse(timeoutOk(10L * 60 * 1000))
        assertFalse(timeoutOk(0))
        assertFalse(timeoutOk(-1))
    }
}

class ScreensaverNextStepTest {
    @Test fun `the steps come in order`() {
        assertEquals(ScreensaverNext.CHOOSE, nextScreensaverStep(selected = false, timeoutOk = true, appeared = false))
        assertEquals(ScreensaverNext.TIMEOUT, nextScreensaverStep(selected = true, timeoutOk = false, appeared = false))
        assertEquals(ScreensaverNext.WAIT, nextScreensaverStep(selected = true, timeoutOk = true, appeared = false))
        assertEquals(ScreensaverNext.DONE, nextScreensaverStep(selected = true, timeoutOk = true, appeared = true))
    }
}
