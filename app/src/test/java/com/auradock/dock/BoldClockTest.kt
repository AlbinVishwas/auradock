package com.auradock.dock

import com.auradock.ui.theme.AuraDockBold
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BoldClockTest {
    private val zone = ZoneId.of("UTC")
    private fun at(h: Int, m: Int) = ZonedDateTime.of(2026, 11, 7, h, m, 0, 0, zone).toInstant().toEpochMilli()

    @Test fun `12-hour has no leading zero`() {
        assertEquals("9:41", boldTimeString(at(9, 41), false, zone))
        assertEquals("12:30", boldTimeString(at(12, 30), false, zone))
        assertEquals("10:08", boldTimeString(at(10, 8), false, zone))
    }

    @Test fun `12-hour afternoon and midnight`() {
        assertEquals("1:07", boldTimeString(at(13, 7), false, zone))
        assertEquals("12:05", boldTimeString(at(0, 5), false, zone))
    }

    @Test fun `24-hour keeps the leading zero`() {
        assertEquals("07:05", boldTimeString(at(7, 5), true, zone))
        assertEquals("13:07", boldTimeString(at(13, 7), true, zone))
        assertEquals("00:00", boldTimeString(at(0, 0), true, zone))
    }

    @Test fun `rotations follow the style guide and stay within 3 to 5 degrees`() {
        assertEquals(-3f, AuraDockBold.rotationDegrees('1'))
        assertEquals(5f, AuraDockBold.rotationDegrees('2'))
        assertEquals(-4f, AuraDockBold.rotationDegrees('3'))
        assertEquals(5f, AuraDockBold.rotationDegrees('0'))
        "0123456789".forEach { assertTrue(Math.abs(AuraDockBold.rotationDegrees(it)) in 3f..5f) }
    }
}
