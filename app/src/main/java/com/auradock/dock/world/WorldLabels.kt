package com.auradock.dock.world

/** A rectangle in the design frame's units. */
data class Box(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    fun overlaps(o: Box, gap: Float = 0f): Boolean =
        left < o.right + gap && o.left < right + gap && top < o.bottom + gap && o.top < bottom + gap
}

/** A city label to place: its dot's centre and the label's size. */
data class LabelRequest(val id: String, val dotX: Float, val dotY: Float, val width: Float, val height: Float)

/** Where a label went, and its box. */
data class PlacedLabel(val id: String, val box: Box)

/**
 * Places city labels so none overlaps another, a dot or the home label, and all stay inside [bounds]. The frames put a label to the right of its
 * dot, vertically centred; when that does not fit it tries the left, then above and below, then steps further up and down.
 */
fun placeLabels(requests: List<LabelRequest>, obstacles: List<Box>, bounds: Box, dotRadius: Float = 5f, gap: Float = 2f): List<PlacedLabel> {
    val dots = requests.map { Box(it.dotX - dotRadius, it.dotY - dotRadius, it.dotX + dotRadius, it.dotY + dotRadius) }
    val placed = ArrayList<PlacedLabel>()
    requests.forEachIndexed { index, r ->
        val taken = obstacles + dots.filterIndexed { i, _ -> i != index } + placed.map { it.box }
        val offset = dotRadius + 4f
        val h = r.height
        val w = r.width
        val candidates = buildList {
            add(Box(r.dotX + offset, r.dotY - h / 2, r.dotX + offset + w, r.dotY + h / 2)) // right, as in the frames
            add(Box(r.dotX - offset - w, r.dotY - h / 2, r.dotX - offset, r.dotY + h / 2)) // left
            for (step in 1..4) {
                val dy = step * (h + gap)
                add(Box(r.dotX + offset, r.dotY - h / 2 - dy, r.dotX + offset + w, r.dotY + h / 2 - dy)) // right, higher
                add(Box(r.dotX + offset, r.dotY - h / 2 + dy, r.dotX + offset + w, r.dotY + h / 2 + dy)) // right, lower
                add(Box(r.dotX - offset - w, r.dotY - h / 2 - dy, r.dotX - offset, r.dotY + h / 2 - dy))
                add(Box(r.dotX - offset - w, r.dotY - h / 2 + dy, r.dotX - offset, r.dotY + h / 2 + dy))
            }
        }
        val fits = candidates.firstOrNull { c ->
            c.left >= bounds.left && c.right <= bounds.right && c.top >= bounds.top && c.bottom <= bounds.bottom && taken.none { it.overlaps(c, gap) }
        }
        // Nothing fits (a crowded map): keep the first position inside the frame rather than drop the label.
        val box = fits ?: candidates.first().let {
            val dx = (bounds.left - it.left).coerceAtLeast(0f) - (it.right - bounds.right).coerceAtLeast(0f)
            Box(it.left + dx, it.top, it.right + dx, it.bottom)
        }
        placed += PlacedLabel(r.id, box)
    }
    return placed
}
