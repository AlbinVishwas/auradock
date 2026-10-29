package com.auradock.tasks.reminders

import com.auradock.tasks.TaskEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class AlarmPlanTest {
    private val now = 1_000_000L
    private fun task(dueAt: Long?, done: Boolean = false) =
        TaskEntity(id = 1, title = "t", dueAt = dueAt, done = done, createdAt = 0, completedAt = if (done) 1 else null)

    @Test
    fun `a future time with exact alarms allowed gets an exact alarm`() =
        assertEquals(AlarmPlan.Exact(now + 120_000), alarmPlan(task(now + 120_000), now, canScheduleExact = true))

    @Test
    fun `without the permission it falls back to an inexact alarm`() =
        assertEquals(AlarmPlan.Inexact(now + 120_000), alarmPlan(task(now + 120_000), now, canScheduleExact = false))

    @Test
    fun `no time means no alarm`() =
        assertEquals(AlarmPlan.None, alarmPlan(task(null), now, true))

    @Test
    fun `a finished task gets no alarm, which cancels an existing one`() =
        assertEquals(AlarmPlan.None, alarmPlan(task(now + 120_000, done = true), now, true))

    @Test
    fun `a time that has already passed gets no alarm`() {
        assertEquals(AlarmPlan.None, alarmPlan(task(now - 1), now, true))
        assertEquals(AlarmPlan.None, alarmPlan(task(now), now, true)) // due this very moment: too late to schedule
    }
}
