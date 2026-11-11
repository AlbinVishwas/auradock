package com.auradock.analytics

/**
 * The one gate every analytics event passes before it can leave the phone (PostHog). It is an allow-list, so anything that is not
 * explicitly known is dropped:
 *  - an event name must be one of [EVENT_PROPERTIES];
 *  - a property key must be listed for that event;
 *  - a property value must be a boolean, a count (a whole number from 0 to [MAX_COUNT]), or one of the fixed codes listed for that key.
 * Free text (a task or intention title, notification text, a contact or event name, anything typed) is never in a list, so it cannot
 * pass, whatever the property is called.
 */
object AnalyticsScrubber {
    const val MAX_COUNT = 10_000L

    /** A duration is only ever sent as one of these buckets. */
    enum class DurationBucket(val code: String) {
        UNDER_5("under_5_min"), FROM_5_TO_30("5_to_30_min"), FROM_30_TO_120("30_to_120_min"), OVER_120("over_120_min");

        companion object {
            fun of(seconds: Long): DurationBucket = when {
                seconds < 5 * 60 -> UNDER_5
                seconds < 30 * 60 -> FROM_5_TO_30
                seconds <= 120 * 60 -> FROM_30_TO_120
                else -> OVER_120
            }
        }
    }

    /** The fixed codes each enum-valued key may carry. Values are lower case. */
    val CODES: Map<String, Set<String>> = mapOf(
        "step" to setOf("welcome", "preview", "template", "screensaver", "anywhere", "done"),
        "permission" to setOf("calendar", "notification_access", "dnd_access", "location", "post_notifications", "exact_alarms", "contacts"),
        "source" to setOf("dock_now", "screensaver", "watcher"),
        "template" to setOf("bold", "classic", "focus", "minimal", "productivity", "world", "split"),
        "widget" to setOf("dial", "bold_time", "digital", "month", "big_date", "up_next", "weather", "now_playing", "tasks", "focus"),
        "side" to setOf("left", "right"),
        "duration" to DurationBucket.entries.map { it.code }.toSet(),
        "environment" to setOf("dev")
    )

    /** Keys whose value is a count. */
    private val COUNT_KEYS = setOf("count")

    /** Keys whose value is a boolean. */
    private val BOOLEAN_KEYS = setOf("exact")

    /** Every event that may be sent, with the property keys each may carry. */
    val EVENT_PROPERTIES: Map<String, Set<String>> = mapOf(
        AnalyticsEvents.ONBOARDING_STEP to setOf("step"),
        AnalyticsEvents.PERMISSION_GRANTED to setOf("permission"),
        AnalyticsEvents.PERMISSION_SKIPPED to setOf("permission"),
        AnalyticsEvents.SCREENSAVER_SET to emptySet(),
        AnalyticsEvents.FIRST_DOCK to setOf("source"),
        AnalyticsEvents.TEMPLATE_CHANGED to setOf("template"),
        AnalyticsEvents.SESSION_START to setOf("source"),
        AnalyticsEvents.SESSION_END to setOf("duration"),
        AnalyticsEvents.TASK_ADDED to emptySet(),
        AnalyticsEvents.TASK_DONE to emptySet(),
        AnalyticsEvents.INTENTION_SET to emptySet(),
        AnalyticsEvents.INTENTION_DONE to emptySet(),
        AnalyticsEvents.SHUTDOWN_USED to emptySet(),
        AnalyticsEvents.FOCUS_UNDONE to emptySet(),
        AnalyticsEvents.WIDGET_ADDED to setOf("widget", "side"),
        AnalyticsEvents.WIDGET_REMOVED to setOf("widget", "side"),
        AnalyticsEvents.WIDGET_REORDERED to setOf("side")
    )

    fun isAllowedEvent(name: String): Boolean = name in EVENT_PROPERTIES

    /** Whether [value] may be sent as property [key]. */
    fun isAllowedValue(key: String, value: Any?): Boolean = when {
        key in COUNT_KEYS -> (value is Int || value is Long) && (value as Number).toLong() in 0..MAX_COUNT
        key in BOOLEAN_KEYS -> value is Boolean
        key in CODES -> value is String && value in CODES.getValue(key)
        else -> false
    }

    /**
     * What may be sent for [event] with [properties]: null if the event itself is not allowed (it is dropped whole), otherwise only the
     * properties that are listed for it and whose value is allowed. A rejected property is dropped, never sent in another form.
     */
    fun clean(event: String, properties: Map<String, Any?>): Map<String, Any>? {
        val allowedKeys = EVENT_PROPERTIES[event] ?: return null
        val out = LinkedHashMap<String, Any>()
        for ((key, value) in properties) {
            if (key in allowedKeys && value != null && isAllowedValue(key, value)) out[key] = value
        }
        return out
    }

    /**
     * Turns what the local event log keeps (the event name and its single `detail` text) into the properties to send. The detail is
     * mapped to a code, a bucket or a count, never passed through: anything unknown becomes no property at all.
     */
    fun fromLocalEvent(name: String, detail: String?): Map<String, Any?> = when (name) {
        AnalyticsEvents.ONBOARDING_STEP -> mapOf("step" to detail?.lowercase())
        AnalyticsEvents.PERMISSION_GRANTED, AnalyticsEvents.PERMISSION_SKIPPED -> mapOf("permission" to detail?.lowercase())
        AnalyticsEvents.FIRST_DOCK, AnalyticsEvents.SESSION_START -> mapOf("source" to detail?.lowercase())
        AnalyticsEvents.TEMPLATE_CHANGED -> mapOf("template" to detail?.lowercase())
        AnalyticsEvents.SESSION_END -> mapOf("duration" to detail?.toLongOrNull()?.let { DurationBucket.of(it).code })
        AnalyticsEvents.WIDGET_ADDED, AnalyticsEvents.WIDGET_REMOVED -> {
            val parts = detail?.split(":").orEmpty()
            mapOf("widget" to parts.getOrNull(0)?.lowercase(), "side" to parts.getOrNull(1)?.lowercase())
        }
        AnalyticsEvents.WIDGET_REORDERED -> mapOf("side" to detail?.lowercase())
        else -> emptyMap()
    }
}
