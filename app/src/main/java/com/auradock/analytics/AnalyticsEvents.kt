package com.auradock.analytics

/**
 * The names of the events AuraDock records locally (see [EventLog]). They are written the way Firebase Analytics wants event names
 * (lower case, underscores, under 40 characters) so they can be sent as they are if analytics is ever added; each has at most one
 * parameter, kept in the table's `detail` column. 
 *
 * | Event                | Parameter (detail)                          |
 * |----------------------|---------------------------------------------|
 * | onboarding_step      | step: welcome, preview, template, screensaver, anywhere, done |
 * | permission_granted   | permission: CALENDAR, DND_ACCESS, ...       |
 * | permission_skipped   | permission                                  |
 * | screensaver_set      | (none)                                      |
 * | first_dock           | source: DOCK_NOW, SCREENSAVER, WATCHER      |
 * | template_changed     | template: BOLD, CLASSIC, ...                |
 * | session_start        | source                                      |
 * | session_end          | duration_s: whole seconds                   |
 * | task_added           | (none)                                      |
 * | task_done            | (none)                                      |
 * | intention_set        | (none)                                      |
 * | intention_done       | (none)                                      |
 * | shutdown_used        | (none)                                      |
 * | focus_undone         | (none)                                      |
 * | widget_added         | widget: WidgetKind, side: left or right     |
 * | widget_removed       | widget, side                                |
 * | widget_reordered     | side                                        |
 *
 * Since 0.3.2 the same events are also forwarded to PostHog (see [Analytics]) with enums, counts and duration buckets only.
 */
object AnalyticsEvents {
    const val ONBOARDING_STEP = "onboarding_step"
    const val PERMISSION_GRANTED = "permission_granted"
    const val PERMISSION_SKIPPED = "permission_skipped"
    const val SCREENSAVER_SET = "screensaver_set"
    const val FIRST_DOCK = "first_dock"
    const val TEMPLATE_CHANGED = "template_changed"
    const val SESSION_START = "session_start"
    const val SESSION_END = "session_end"
    const val TASK_ADDED = "task_added"
    const val TASK_DONE = "task_done"
    const val INTENTION_SET = "intention_set"
    const val INTENTION_DONE = "intention_done"
    const val SHUTDOWN_USED = "shutdown_used"
    const val FOCUS_UNDONE = "focus_undone"
    const val WIDGET_ADDED = "widget_added"
    const val WIDGET_REMOVED = "widget_removed"
    const val WIDGET_REORDERED = "widget_reordered"
}
