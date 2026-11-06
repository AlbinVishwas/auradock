package com.auradock.dock

/** The dock looks V1 offers. No customization: the user just picks one. */
enum class DockTemplate(val label: String, val description: String) {
    BOLD("Bold", "Just the time, big and bold"),
    CLASSIC("Classic", "Big date, city and temperature"),
    FOCUS("Focus", "Clock, next event and top task"),
    MINIMAL("Minimal", "Just the time, dimmed for OLED"),
    PRODUCTIVITY("Productivity", "Clock, tasks, events and weather");

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
