package com.auradock.intention

import com.auradock.tasks.TaskEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ResumeRulesTest {
    private val now = 1_000_000_000L
    private fun task(title: String, touchedAgo: Long, done: Boolean = false) =
        TaskEntity(title = title, createdAt = now - touchedAgo, done = done, touchedAt = now - touchedAgo)

    @Test
    fun `an unfinished intention comes first`() {
        assertEquals("Ship it", resumeTitle(IntentionLine("Ship it", false), listOf(task("Other", 1_000)), now))
    }

    @Test
    fun `a finished intention falls back to the task touched last`() {
        val tasks = listOf(task("Older", 5 * 3_600_000L), task("Newest", 3_600_000L), task("Done", 1_000, done = true))
        assertEquals("Newest", resumeTitle(IntentionLine("Ship it", true), tasks, now))
        assertEquals("Newest", resumeTitle(null, tasks, now))
    }

    @Test
    fun `nothing when everything is done or too old`() {
        assertNull(resumeTitle(null, listOf(task("Done", 1_000, done = true)), now))
        assertNull(resumeTitle(null, listOf(task("Stale", RESUME_MAX_AGE_MS + 1)), now))
        assertNull(resumeTitle(null, emptyList(), now))
    }
}
