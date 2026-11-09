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

    // A made-up font: every digit has the same height, but widths and side bearings differ, as in a real one.
    private val inks = mapOf(
        '0' to Ink(0.05f, -0.72f, 0.60f, 0.01f), '1' to Ink(0.10f, -0.71f, 0.33f, 0f), '2' to Ink(0.04f, -0.72f, 0.54f, 0f),
        '3' to Ink(0.04f, -0.72f, 0.52f, 0.01f), '4' to Ink(0.02f, -0.71f, 0.56f, 0f), '5' to Ink(0.05f, -0.70f, 0.53f, 0.01f),
        '7' to Ink(0.03f, -0.71f, 0.52f, 0f), '9' to Ink(0.04f, -0.72f, 0.55f, 0.01f)
    )
    private fun plan(t: String) = planBold(t) { inks.getValue(it) }

    @Test fun `every digit pair overlaps by the same amount of ink`() {
        val p = plan("12:57")
        val d = p.digits
        val pairs = listOf(0 to 1, 2 to 3)
        pairs.forEach { (a, b) ->
            val overlap = (d[a].penX + d[a].ink.right) - (d[b].penX + d[b].ink.left)
            assertEquals(AuraDockBold.OverlapEm, overlap, 1e-5f)
        }
    }

    @Test fun `the colon sits in the gap and reaches 30 percent into each digit`() {
        val p = plan("12:57")
        val left = p.digits[1].penX + p.digits[1].ink.right
        val right = p.digits[2].penX + p.digits[2].ink.left
        val r = AuraDockBold.ColonDiameterEm / 2f
        assertEquals((left + right) / 2f, p.colonX!!, 1e-5f)
        assertEquals(AuraDockBold.ColonOverlap * AuraDockBold.ColonDiameterEm, (p.colonX!! + r) - right, 1e-5f)
        assertEquals(AuraDockBold.ColonOverlap * AuraDockBold.ColonDiameterEm, left - (p.colonX!! - r), 1e-5f)
    }

    @Test fun `colon dots sit at a third and two thirds of the digit height`() {
        val p = plan("12:57")
        assertEquals(p.top + 0.33f * p.height, p.colonUpperY, 1e-5f)
        assertEquals(p.top + 0.67f * p.height, p.colonLowerY, 1e-5f)
    }

    @Test fun `three-digit times come out larger than four-digit ones`() {
        val three = boldFontSizePx(plan("9:57"), 700f, 800f)
        val four = boldFontSizePx(plan("12:57"), 700f, 800f)
        assertTrue(three > four)
    }

    @Test fun `width fills 90 percent of the safe width, height is capped at 80 percent`() {
        val p = plan("12:57")
        val wide = boldFontSizePx(p, 700f, 2000f)
        assertEquals(0.9f * 700f, wide * p.width, 0.01f)
        val short = boldFontSizePx(p, 2000f, 200f)
        assertEquals(0.8f * 200f, short * p.height, 0.01f)
    }

    @Test fun `tilts follow the style guide and stay within 3 to 5 degrees`() {
        assertEquals(-3f, AuraDockBold.tiltDegrees('1'))
        assertEquals(5f, AuraDockBold.tiltDegrees('2'))
        assertEquals(-4f, AuraDockBold.tiltDegrees('3'))
        assertEquals(5f, AuraDockBold.tiltDegrees('0'))
        "0123456789".forEach { assertTrue(Math.abs(AuraDockBold.tiltDegrees(it)) in 3f..5f) }
    }
}
