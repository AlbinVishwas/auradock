package com.auradock.dock.split

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SplitModelTest {
    @Test fun `the default is the Dial on the left and the Month on the right`() {
        val d = SplitStacks()
        assertEquals(listOf(WidgetKind.DIAL), d.left)
        assertEquals(listOf(WidgetKind.MONTH), d.right)
    }

    @Test fun `stacks survive saving and a damaged value falls back to the default`() {
        val s = SplitStacks(listOf(WidgetKind.DIGITAL, WidgetKind.WEATHER), listOf(WidgetKind.TASKS))
        assertEquals(s, decodeStacks(encodeStacks(s)))
        assertEquals(SplitStacks(), decodeStacks(null))
        assertEquals(SplitStacks(), decodeStacks("garbage"))
        assertEquals(listOf(WidgetKind.DIAL), decodeStacks("L:NOPE|R:MONTH").left)
    }

    @Test fun `swiping goes forward or back and wraps`() {
        assertEquals(1, cycleIndex(0, 3, true))
        assertEquals(0, cycleIndex(2, 3, true))
        assertEquals(2, cycleIndex(0, 3, false))
        assertEquals(0, cycleIndex(0, 1, true))
    }

    @Test fun `the corner time shows only when neither half shows a time widget`() {
        assertFalse(needsCornerTime(WidgetKind.DIAL, WidgetKind.MONTH))
        assertFalse(needsCornerTime(WidgetKind.MONTH, WidgetKind.DIGITAL))
        assertTrue(needsCornerTime(WidgetKind.MONTH, WidgetKind.TASKS))
        assertTrue(needsCornerTime(null, null))
    }

    @Test fun `Now playing is hidden from the stack while nothing plays`() {
        val stack = listOf(WidgetKind.DIAL, WidgetKind.NOW_PLAYING, WidgetKind.MONTH)
        assertEquals(listOf(WidgetKind.DIAL, WidgetKind.MONTH), visibleStack(stack, nowPlaying = false))
        assertEquals(stack, visibleStack(stack, nowPlaying = true))
        assertEquals(listOf(WidgetKind.NOW_PLAYING), visibleStack(listOf(WidgetKind.NOW_PLAYING), nowPlaying = false))
    }

    @Test fun `a widget on one side is not offered on the other`() {
        val stacks = SplitStacks(listOf(WidgetKind.DIAL), listOf(WidgetKind.MONTH))
        val forRight = availableToAdd(stacks, SplitSide.RIGHT)
        assertFalse(WidgetKind.DIAL in forRight) // on the left
        assertFalse(WidgetKind.MONTH in forRight) // already here
        assertTrue(WidgetKind.WEATHER in forRight)
        assertFalse(WidgetKind.MONTH in availableToAdd(stacks, SplitSide.LEFT)) // the Month is on the right
    }

    @Test fun `there is no World widget in the catalog (World is its own full-screen template)`() {
        assertTrue(WidgetKind.entries.none { it.name == "WORLD" })
        assertEquals(listOf(WidgetKind.DIAL), decodeStacks("L:DIAL,WORLD|R:MONTH").left) // an old saved value drops it
    }

    @Test fun `saved stacks never hold a widget on both sides`() {
        val s = decodeStacks("L:DIAL,MONTH|R:MONTH,WEATHER")
        assertEquals(listOf(WidgetKind.WEATHER), s.right)
        assertEquals(listOf(WidgetKind.DIAL, WidgetKind.MONTH), s.left)
    }

    @Test fun `adding removing and moving widgets`() {
        val s = listOf(WidgetKind.DIAL, WidgetKind.MONTH)
        assertEquals(s, addWidget(s, WidgetKind.DIAL))
        assertEquals(s + WidgetKind.WEATHER, addWidget(s, WidgetKind.WEATHER))
        assertEquals(listOf(WidgetKind.MONTH), removeWidget(s, WidgetKind.DIAL))
        assertEquals(listOf(WidgetKind.MONTH), removeWidget(listOf(WidgetKind.MONTH), WidgetKind.MONTH)) // never the last one
        assertEquals(listOf(WidgetKind.MONTH, WidgetKind.DIAL), moveWidget(s, 0, 1))
        assertEquals(s, moveWidget(s, 0, 5))
    }

    @Test fun `a swipe fades the old widget out faster and the new one in, with the blur easing off`() {
        val half = swipeLook(0.4f, 1.8f, 1.5f, 10f)
        assertEquals(0.28f, half.outgoingAlpha, 1e-4f) // the frame: 28%
        assertEquals(0.6f, half.incomingAlpha, 1e-4f) // the frame: 60%
        assertEquals(6f, half.incomingBlurDp, 1e-4f) // the frame: blur 6
        val done = swipeLook(1f, 1.8f, 1.5f, 10f)
        assertEquals(0f, done.outgoingAlpha)
        assertEquals(1f, done.incomingAlpha)
        assertEquals(0f, done.incomingBlurDp)
    }
}

class MonthGridTest {
    @Test fun `November 2026 on a Monday-first calendar starts in the seventh cell like the frame`() {
        val cells = monthCells(java.time.YearMonth.of(2026, 11), java.time.DayOfWeek.MONDAY)
        assertEquals(42, cells.size) // six rows, as the frame draws it
        assertEquals(List(6) { null }, cells.take(6)) // the frame: six empty cells, then 1 on the Sunday
        assertEquals(1, cells[6])
        assertEquals(30, cells.filterNotNull().last())
    }

    @Test fun `a Sunday-first calendar puts November 1st in the first cell`() {
        val cells = monthCells(java.time.YearMonth.of(2026, 11), java.time.DayOfWeek.SUNDAY)
        assertEquals(1, cells[0])
        assertEquals(35, cells.size)
    }

    @Test fun `weekday initials follow the first day`() {
        assertEquals(listOf("M", "T", "W", "T", "F", "S", "S"), weekdayInitials(java.time.DayOfWeek.MONDAY, java.util.Locale.ENGLISH))
        assertEquals(listOf("S", "M", "T", "W", "T", "F", "S"), weekdayInitials(java.time.DayOfWeek.SUNDAY, java.util.Locale.ENGLISH))
    }
}

class WidgetTextTest {
    @Test fun `countdown reads in minutes then hours`() {
        assertEquals("in 25 min", countdownText(25 * 60_000L, 0L))
        assertEquals("in 1 min", countdownText(30_000L, 0L)) // rounded up, as the dock's own countdown is
        assertEquals("in 1 h 5 min", countdownText(65 * 60_000L, 0L))
        assertEquals("in 2 h", countdownText(120 * 60_000L, 0L))
    }

    @Test fun `the focus clock shows minutes and seconds, then hours`() {
        assertEquals("42:10", focusClock(42 * 60_000L + 10_000L))
        assertEquals("0:00", focusClock(0L))
        assertEquals("1:02:10", focusClock(3_730_000L))
    }

    @Test fun `track progress moves while playing and holds while paused`() {
        val s = NowPlayingState("T", "A", null, null, "Music", playing = true, durationMs = 100_000L, positionMs = 20_000L, positionAt = 1_000L, speed = 1f)
        assertEquals(0.2f, s.progress(1_000L), 1e-4f)
        assertEquals(0.3f, s.progress(11_000L), 1e-4f)
        assertEquals(0.2f, s.copy(playing = false).progress(11_000L), 1e-4f)
        assertEquals(0f, s.copy(durationMs = 0L).progress(5_000L), 1e-4f)
    }
}
