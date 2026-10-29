package com.auradock.tasks

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class TaskRulesTest {
    private val zone = ZoneId.of("Asia/Kolkata")
    private val day = LocalDate.of(2026, 10, 29)
    private fun at(date: LocalDate, h: Int, m: Int = 0) = date.atTime(h, m).atZone(zone).toInstant().toEpochMilli()
    private val midnight = day.atStartOfDay(zone).toInstant().toEpochMilli()

    private fun task(
        id: Long = 1, title: String = "t", dueAt: Long? = null, done: Boolean = false,
        createdAt: Long = at(day, 8), completedAt: Long? = null
    ) = TaskEntity(id, title, dueAt, done, createdAt, completedAt)

    // ── rule 1: unfinished tasks carry over ───────────────────────────────────

    @Test
    fun `an unfinished task from days ago is still shown today`() {
        val old = task(createdAt = at(day.minusDays(5), 9))
        assertTrue(isVisibleToday(old, midnight))
    }

    // ── rule 2: finished tasks vanish at midnight ─────────────────────────────

    @Test
    fun `a task finished earlier today stays visible`() =
        assertTrue(isVisibleToday(task(done = true, completedAt = at(day, 7)), midnight))

    @Test
    fun `a task finished yesterday is hidden`() =
        assertFalse(isVisibleToday(task(done = true, completedAt = at(day.minusDays(1), 23, 59)), midnight))

    @Test
    fun `a task finished exactly at midnight belongs to the new day`() =
        assertTrue(isVisibleToday(task(done = true, completedAt = midnight), midnight))

    @Test
    fun `the same finished task is shown at 23h59 and hidden the next day`() {
        val finished = task(done = true, completedAt = at(day, 20))
        assertTrue(isVisibleToday(finished, startOfToday(at(day, 23, 59), zone)))
        assertFalse(isVisibleToday(finished, startOfToday(at(day.plusDays(1), 0, 1), zone)))
    }

    @Test
    fun `un-checking a finished task brings it back as unfinished`() =
        assertTrue(isVisibleToday(task(done = false, completedAt = null), midnight + 10 * 3_600_000L))

    @Test
    fun `visibleTasks filters out old finished tasks`() {
        val shown = visibleTasks(
            listOf(
                task(1, "carried over", createdAt = at(day.minusDays(2), 9)),
                task(2, "done yesterday", done = true, completedAt = at(day.minusDays(1), 18)),
                task(3, "done today", done = true, completedAt = at(day, 9))
            ),
            midnight
        )
        assertEquals(listOf("carried over", "done today"), shown.map { it.title })
    }

    // ── overdue ───────────────────────────────────────────────────────────────

    @Test
    fun `a task is overdue once its time has come`() {
        val t = task(dueAt = at(day, 18, 30))
        assertFalse(isOverdue(t, at(day, 18, 29)))
        assertTrue(isOverdue(t, at(day, 18, 30)))
        assertTrue(isOverdue(t, at(day, 19)))
    }

    @Test
    fun `finished and untimed tasks are never overdue`() {
        assertFalse(isOverdue(task(dueAt = at(day, 8), done = true, completedAt = at(day, 9)), at(day, 20)))
        assertFalse(isOverdue(task(dueAt = null), at(day, 20)))
    }

    // ── ordering ──────────────────────────────────────────────────────────────

    @Test
    fun `unfinished first by due time, untimed after them, finished last`() {
        val ordered = visibleTasks(
            listOf(
                task(1, "no time A", createdAt = at(day, 8)),
                task(2, "late", dueAt = at(day, 17)),
                task(3, "done", done = true, completedAt = at(day, 9)),
                task(4, "early", dueAt = at(day, 10)),
                task(5, "no time B", createdAt = at(day, 9))
            ),
            midnight
        )
        assertEquals(listOf("early", "late", "no time A", "no time B", "done"), ordered.map { it.title })
    }

    @Test
    fun `finished tasks keep the order they were finished in`() {
        val ordered = visibleTasks(
            listOf(
                task(1, "second", done = true, completedAt = at(day, 12)),
                task(2, "first", done = true, completedAt = at(day, 9))
            ),
            midnight
        )
        assertEquals(listOf("first", "second"), ordered.map { it.title })
    }

    // ── due time today ────────────────────────────────────────────────────────

    @Test
    fun `a picked time means that time today`() =
        assertEquals(at(day, 14, 5), dueTimeToday(14, 5, at(day, 9), zone))

    @Test
    fun `a time that has already passed still means earlier today`() =
        assertEquals(at(day, 7, 0), dueTimeToday(7, 0, at(day, 15), zone))

    @Test
    fun `start of today uses the local day`() {
        // 23:30 local on the 29th is still the 29th, even though it is already the 30th in UTC+14
        assertEquals(midnight, startOfToday(at(day, 23, 30), zone))
    }

    // ── titles ────────────────────────────────────────────────────────────────

    @Test
    fun `titles are trimmed, blank ones rejected, long ones cut`() {
        assertEquals("Call mum", cleanTitle("  Call mum  "))
        assertNull(cleanTitle("   "))
        assertEquals(MAX_TITLE_LENGTH, cleanTitle("x".repeat(500))!!.length)
    }

    // ── priority (what the dock shows first) ──────────────────────────────────

    @Test
    fun `priority is overdue first, then earliest due time, then tasks with no time oldest first`() {
        val now = at(day, 12)
        val order = priorityOrder(
            listOf(
                task(1, "no time, new", createdAt = at(day, 11)),
                task(2, "due 16:00", dueAt = at(day, 16)),
                task(3, "overdue 11:30", dueAt = at(day, 11, 30)),
                task(4, "no time, old", createdAt = at(day.minusDays(1), 9)),
                task(5, "due 14:00", dueAt = at(day, 14)),
                task(6, "overdue 09:00", dueAt = at(day, 9))
            ),
            now
        )
        assertEquals(
            listOf("overdue 09:00", "overdue 11:30", "due 14:00", "due 16:00", "no time, old", "no time, new"),
            order.map { it.title }
        )
    }

    @Test
    fun `finished tasks are never prioritised`() {
        val order = priorityOrder(listOf(task(1, "done", done = true, completedAt = at(day, 9)), task(2, "open")), at(day, 12))
        assertEquals(listOf("open"), order.map { it.title })
    }

    @Test
    fun `top task is the first in priority order, or null when nothing is left`() {
        assertEquals("overdue", topTask(listOf(task(1, "later", dueAt = at(day, 18)), task(2, "overdue", dueAt = at(day, 8))), at(day, 12))?.title)
        assertNull(topTask(emptyList(), at(day, 12)))
        assertNull(topTask(listOf(task(1, "done", done = true, completedAt = at(day, 9))), at(day, 12)))
    }
}
