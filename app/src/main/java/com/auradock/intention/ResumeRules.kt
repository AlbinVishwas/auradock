package com.auradock.intention

import com.auradock.tasks.TaskEntity

/** How long the "Pick up" line stays after a dock opens. */
const val RESUME_SHOWN_MS = 30_000L

/** A task last touched longer ago than this is not "what you were doing". */
const val RESUME_MAX_AGE_MS = 24 * 60 * 60_000L

/**
 * What to offer on re-docking, or null: the morning intention while it is not done; otherwise the task touched most recently
 * (made, edited or un-ticked within a day) that is not done. A finished one is never offered.
 */
fun resumeTitle(intention: IntentionLine?, tasks: List<TaskEntity>, nowMillis: Long): String? {
    if (intention != null && !intention.done) return intention.text
    return tasks
        .filter { !it.done && nowMillis - it.touchedAt <= RESUME_MAX_AGE_MS }
        .maxByOrNull { it.touchedAt }?.title
}
