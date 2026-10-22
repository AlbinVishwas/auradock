package com.auradock.time

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class MinuteClockTest {

    // 2026-10-22 17:47:30 UTC — 30 seconds into a minute
    private val start = 1_792_691_250_000L

    @Test
    fun `emits immediately then at each minute boundary`() = runTest {
        // Virtual time: "now" is start plus however much virtual time has passed.
        val clock = MinuteClock { start + currentTime }
        val seen = mutableListOf<Long>()
        backgroundScope.launch { clock.minutes.toList(seen) }

        runCurrent()
        assertEquals(listOf(start), seen) // immediate emission

        advanceTimeBy(29_999)
        runCurrent()
        assertEquals(1, seen.size) // nothing yet, 1 ms before the boundary

        advanceTimeBy(1)
        runCurrent()
        assertEquals(2, seen.size)
        assertEquals(0L, seen[1] % 60_000) // second tick lands exactly on hh:mm:00

        advanceTimeBy(60_000)
        runCurrent()
        assertEquals(3, seen.size) // then once a minute
        assertEquals(0L, seen[2] % 60_000)
    }

    @Test
    fun `does not tick every second`() = runTest {
        val clock = MinuteClock { start + currentTime }
        val seen = mutableListOf<Long>()
        backgroundScope.launch { clock.minutes.toList(seen) }
        advanceTimeBy(10 * 60_000L)
        runCurrent()
        assertEquals(11, seen.size) // 1 immediate + 10 minutes, not 600
    }

    @Test
    fun `formats the dashboard date in the given zone`() {
        val utc = formatDashboardDate(start, ZoneId.of("UTC"), Locale.US)
        assertEquals("Thursday • October 22", utc)
        // 17:47 UTC is already the next day in Auckland (UTC+13 in October)
        val nz = formatDashboardDate(start, ZoneId.of("Pacific/Auckland"), Locale.US)
        assertEquals("Friday • October 23", nz)
    }
}
