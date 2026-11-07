package com.auradock.dock

/** The dock looks V1 offers. No customization: the user just picks one. */
enum class DockTemplate(val label: String, val description: String, val shows: String, val bestFor: String) {
    BOLD("Bold", "A giant two-tone clock. Nothing else.", "Time, one message line", "Reading across the room"),
    CLASSIC("Classic", "Big date, city and temperature.", "Date, city, temperature", "A glance at the day"),
    FOCUS("Focus", "Giant digits, your next event and top task.", "Time, next event, top task", "Working at a desk"),
    MINIMAL("Minimal", "Just the time, dimmed for OLED.", "Time, dimmed", "The bedside at night"),
    PRODUCTIVITY("Productivity", "Clock, tasks, events and weather.", "Time, tasks, events, weather", "Planning the day");

    companion object {
        val default = BOLD

        /** Templates the gallery offers, Bold first. */
        val available = listOf(BOLD, CLASSIC, FOCUS, MINIMAL, PRODUCTIVITY)
    }
}

/**
 * Turns a stored name back into a template. Anything missing, unknown, or not offered yet falls back to the
 * default, so an old or corrupted value can never crash the dock or select a template that is not built.
 */
fun parseDockTemplate(stored: String?): DockTemplate =
    DockTemplate.available.firstOrNull { it.name == stored } ?: DockTemplate.default
