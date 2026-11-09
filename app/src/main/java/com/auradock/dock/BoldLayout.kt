package com.auradock.dock

import com.auradock.ui.theme.AuraDockBold

/**
 * The drawn shape of a glyph, in em (1.0 = the font size), measured from the pen position on the baseline. [top] is
 * negative (above the baseline). This is the ink, not the advance width, so side bearings never leak into the layout.
 */
data class Ink(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
}

/** One digit placed by the plan: where its pen goes (em, from the plan's origin) and its ink. */
data class BoldDigit(val char: Char, val penX: Float, val ink: Ink)

/**
 * Where everything sits, in em, in landscape coordinates (baseline at y = 0). The colon is two dots at [colonX], at heights
 * [colonUpperY] and [colonLowerY]. The combined ink box of the digits and the colon is ([left], [top]) to ([right], [bottom]).
 */
data class BoldPlan(
    val digits: List<BoldDigit>,
    val colonX: Float?,
    val colonUpperY: Float,
    val colonLowerY: Float,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f
}

/**
 * Lays the time out by ink. Each digit goes so that its ink overlaps the previous digit's ink by [AuraDockBold.OverlapEm];
 * the colon sits in the gap between the hour and the minute, its dots overlapping the digits either side by
 * [AuraDockBold.ColonOverlap] of a dot's diameter. [inkOf] gives the ink of a digit.
 */
fun planBold(text: String, inkOf: (Char) -> Ink): BoldPlan {
    val digits = ArrayList<BoldDigit>()
    var colonX: Float? = null
    var prevRight = 0f
    var afterColon = false
    var first = true
    val dot = AuraDockBold.ColonDiameterEm
    // The gap the colon sits in: the dots reach into each neighbouring digit by ColonOverlap of their diameter.
    val colonGap = dot * (1f - 2f * AuraDockBold.ColonOverlap)
    for (c in text) {
        if (c == ':') {
            afterColon = true
            continue
        }
        val ink = inkOf(c)
        val penX = when {
            first -> -ink.left
            afterColon -> prevRight + colonGap - ink.left
            else -> prevRight - AuraDockBold.OverlapEm - ink.left
        }
        if (afterColon) colonX = prevRight + colonGap / 2f
        digits += BoldDigit(c, penX, ink)
        prevRight = penX + ink.right
        afterColon = false
        first = false
    }
    if (digits.isEmpty()) return BoldPlan(emptyList(), null, 0f, 0f, 0f, 0f, 0f, 0f)
    val top = digits.minOf { it.ink.top }
    val bottom = digits.maxOf { it.ink.bottom }
    val height = bottom - top
    return BoldPlan(
        digits = digits,
        colonX = colonX,
        colonUpperY = top + AuraDockBold.ColonUpper * height,
        colonLowerY = top + AuraDockBold.ColonLower * height,
        left = digits.first().penX + digits.first().ink.left,
        top = top,
        right = digits.last().penX + digits.last().ink.right,
        bottom = bottom
    )
}

/**
 * The font size in pixels that makes the plan's ink box [AuraDockBold.WidthShare] of the safe width, but no taller than
 * [AuraDockBold.HeightShare] of the safe height. Three-digit times are narrower, so they come out larger than four-digit ones.
 */
fun boldFontSizePx(plan: BoldPlan, safeWidthPx: Float, safeHeightPx: Float): Float {
    if (plan.width <= 0f || plan.height <= 0f) return 0f
    return minOf(AuraDockBold.WidthShare * safeWidthPx / plan.width, AuraDockBold.HeightShare * safeHeightPx / plan.height)
}
