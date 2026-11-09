package com.auradock.dock.split

/** The groups the Add widget sheet lists the widgets under, in the frame's order. */
enum class WidgetGroup(val label: String) { TIME("Time"), DATE("Date"), INFO("Info"), APP("App") }

/** One thing a half of the Split dock can show. Every widget works on either side. */
enum class WidgetKind(val label: String, val group: WidgetGroup, val blurb: String) {
    DIAL("Dial", WidgetGroup.TIME, "Analog clock"),
    BOLD_TIME("Bold time", WidgetGroup.TIME, "Giant two-tone digits"),
    DIGITAL("Digital", WidgetGroup.TIME, "Time and date"),
    MONTH("Month", WidgetGroup.DATE, "This month at a glance"),
    BIG_DATE("Big date", WidgetGroup.DATE, "Today's date, large"),
    UP_NEXT("Up next", WidgetGroup.DATE, "Calendar"),
    WEATHER("Weather", WidgetGroup.INFO, "Now and today's range"),
    NOW_PLAYING("Now playing", WidgetGroup.INFO, "Hides when nothing plays"),
    TASKS("Tasks", WidgetGroup.INFO, "What is left today"),
    FOCUS("Focus", WidgetGroup.INFO, "This session, and what was held");

    /** Time widgets: at least one side shows one, or the small corner time appears. */
    val showsTime: Boolean get() = group == WidgetGroup.TIME

    companion object {
        fun parse(name: String): WidgetKind? = entries.firstOrNull { it.name == name }
    }
}

enum class SplitSide { LEFT, RIGHT }

/** The two independent stacks. Default: the Dial on the left, the Month on the right. */
data class SplitStacks(
    val left: List<WidgetKind> = listOf(WidgetKind.DIAL),
    val right: List<WidgetKind> = listOf(WidgetKind.MONTH)
) {
    fun of(side: SplitSide): List<WidgetKind> = if (side == SplitSide.LEFT) left else right
    fun with(side: SplitSide, stack: List<WidgetKind>): SplitStacks = if (side == SplitSide.LEFT) copy(left = stack) else copy(right = stack)
}

/** Saved form: "L:DIAL,BIG_DATE|R:MONTH". Anything unknown or empty falls back to the default for that side, so a stale value cannot break the dock. */
fun encodeStacks(stacks: SplitStacks): String =
    "L:" + stacks.left.joinToString(",") { it.name } + "|R:" + stacks.right.joinToString(",") { it.name }

fun decodeStacks(stored: String?): SplitStacks {
    val default = SplitStacks()
    if (stored.isNullOrBlank()) return default
    fun side(tag: String, fallback: List<WidgetKind>): List<WidgetKind> {
        val part = stored.split("|").firstOrNull { it.startsWith("$tag:") } ?: return fallback
        val kinds = part.removePrefix("$tag:").split(",").mapNotNull { WidgetKind.parse(it.trim()) }.distinct()
        return kinds.ifEmpty { fallback }
    }
    val left = side("L", default.left)
    // A widget lives on one side only: anything saved on both sides stays on the left.
    val right = side("R", default.right).filter { it !in left }.ifEmpty { default.right.filter { it !in left }.ifEmpty { listOf(side("R", default.right).first()) } }
    return SplitStacks(left, right)
}

/** The widgets a stack really shows: Now playing is left out while nothing plays. Never empty. */
fun visibleStack(stack: List<WidgetKind>, nowPlaying: Boolean): List<WidgetKind> {
    val shown = stack.filter { nowPlaying || it != WidgetKind.NOW_PLAYING }
    return shown.ifEmpty { stack.take(1) }
}

/** The next index in a stack when swiping: up goes forward, down goes back, both wrap around. */
fun cycleIndex(current: Int, size: Int, forward: Boolean): Int =
    if (size <= 1) 0 else if (forward) (current + 1) % size else (current - 1 + size) % size

/** True when neither half shows a time widget: the small corner time appears so the time is never missing. */
fun needsCornerTime(left: WidgetKind?, right: WidgetKind?): Boolean = left?.showsTime != true && right?.showsTime != true

/**
 * The widgets that can still be added to [side]: not already in its stack, and not in the other stack either (a widget lives on one side only, so
 * a Month on the left is not offered on the right).
 */
fun availableToAdd(stacks: SplitStacks, side: SplitSide): List<WidgetKind> {
    val other = stacks.of(if (side == SplitSide.LEFT) SplitSide.RIGHT else SplitSide.LEFT)
    return WidgetKind.entries.filter { it !in stacks.of(side) && it !in other }
}

/** Adds a widget to the end of a stack (not twice). */
fun addWidget(stack: List<WidgetKind>, kind: WidgetKind): List<WidgetKind> = if (kind in stack) stack else stack + kind

/** Removes a widget, but never the last one: a half always shows something. */
fun removeWidget(stack: List<WidgetKind>, kind: WidgetKind): List<WidgetKind> = if (stack.size <= 1) stack else stack - kind

/** Moves the widget at [from] to [to] (drag to reorder). */
fun moveWidget(stack: List<WidgetKind>, from: Int, to: Int): List<WidgetKind> {
    if (from !in stack.indices || to !in stack.indices || from == to) return stack
    return stack.toMutableList().also { it.add(to, it.removeAt(from)) }
}

/** How a swipe looks at [progress] (0 at rest, 1 when the next widget has arrived): the old one fades out fast, the new one fades in and loses its blur. */
data class SwipeLook(val outgoingAlpha: Float, val incomingAlpha: Float, val incomingBlurDp: Float)

fun swipeLook(progress: Float, outgoingSlope: Float, incomingSlope: Float, blurMaxDp: Float): SwipeLook {
    val p = progress.coerceIn(0f, 1f)
    return SwipeLook(
        outgoingAlpha = (1f - outgoingSlope * p).coerceIn(0f, 1f),
        incomingAlpha = (incomingSlope * p).coerceIn(0f, 1f),
        incomingBlurDp = (1f - p) * blurMaxDp
    )
}
