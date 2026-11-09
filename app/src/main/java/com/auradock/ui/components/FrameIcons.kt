package com.auradock.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * The handful of icons the handoff draws itself (24 x 24 grid, round 1.75 lines), where Material's nearest icon is a different shape:
 * the Templates tile, the Dock now pill and the calendar mark. Drawn as strokes and tinted by the Icon that shows them.
 */
object FrameIcons {
    /** A screen-shaped rounded rectangle with a short foot, as the Dock now pill draws it. */
    val DockNow: ImageVector by lazy {
        icon("DockNow", 2f) {
            roundRect(2.5f, 6.5f, 19f, 11f, 3f)
            moveTo(8f, 21f); lineTo(16f, 21f)
        }
    }

    /** The Templates tile: a wide screen and two short bars, as a row of docks. */
    val Templates: ImageVector by lazy {
        icon("Templates", 1.75f) {
            roundRect(2.5f, 6.5f, 13f, 11f, 2.5f)
            moveTo(18.5f, 8.5f); lineTo(18.5f, 15.5f)
            moveTo(21.5f, 10f); lineTo(21.5f, 14f)
        }
    }

    /** The calendar mark on the empty Calendar card. */
    val Calendar: ImageVector by lazy {
        icon("Calendar", 1.75f) {
            roundRect(3.5f, 5f, 17f, 15.5f, 3f)
            moveTo(3.5f, 9.5f); lineTo(20.5f, 9.5f)
            moveTo(8f, 3f); lineTo(8f, 7f)
            moveTo(16f, 3f); lineTo(16f, 7f)
        }
    }

    private fun icon(name: String, width: Float, block: PathBuilder.() -> Unit): ImageVector =
        ImageVector.Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
            .path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = width,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                pathBuilder = block
            )
            .build()

    private fun PathBuilder.roundRect(x: Float, y: Float, w: Float, h: Float, r: Float) {
        moveTo(x + r, y)
        lineTo(x + w - r, y)
        arcTo(r, r, 0f, false, true, x + w, y + r)
        lineTo(x + w, y + h - r)
        arcTo(r, r, 0f, false, true, x + w - r, y + h)
        lineTo(x + r, y + h)
        arcTo(r, r, 0f, false, true, x, y + h - r)
        lineTo(x, y + r)
        arcTo(r, r, 0f, false, true, x + r, y)
        close()
    }
}
