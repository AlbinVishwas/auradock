package com.auradock.dock.world

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.auradock.ui.theme.AuraDockWorld
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * The World widget's half-size map (frame SplitDock-Widgets, 4): the same dots and projection as the full dock, 14 apart, cropped to
 * 405 x 270 and shifted by whole dots so the home place sits about a third of the way in and the grid stays on the land mask. The mask wraps
 * around the earth, so any home works.
 */
object WorldHalfView {
    const val WIDTH = 405f
    const val HEIGHT = 270f
    private const val HOME_X = 129.1f

    /** The vertical shift that puts the grid rows at 7 + 14 k, as the frame draws them. */
    const val SHIFT_Y = -59f

    /** The horizontal shift, a whole number of dots (less one, the frame's phase), that brings [homeLon] near [HOME_X]. */
    fun shiftX(homeLon: Double): Float {
        val k = ((HOME_X - WorldProjection.x(homeLon) + 1f) / WorldProjection.GRID).roundToInt()
        return k * WorldProjection.GRID - 1f
    }
}

/** The dotted land of the half map, lit by the sun at each dot. Positions are in dp (1 design unit = 1 dp). */
internal fun DrawScope.drawLandHalf(u: Float, shiftX: Float, sun: SunPosition, day: Color, night: Color) {
    for (row in 0 until LandMask.ROWS) {
        for (i in -70..130) {
            val x = WorldProjection.GRID_X0 + WorldProjection.GRID * i + shiftX
            if (x < -3f || x > WorldHalfView.WIDTH + 3f) continue
            val y = WorldProjection.GRID_Y0 + WorldProjection.GRID * row + WorldHalfView.SHIFT_Y
            if (y < -3f || y > WorldHalfView.HEIGHT + 3f) continue
            if (!LandMask.isLand(Math.floorMod(i, LandMask.COLUMNS), row)) continue
            val lit = Solar.isDay(sun, WorldProjection.latOfY(WorldProjection.GRID_Y0 + WorldProjection.GRID * row), WorldProjection.lonOfX(WorldProjection.GRID_X0 + WorldProjection.GRID * i))
            drawCircle(if (lit) day else night, AuraDockWorld.LandDotRadius * u, Offset(x * u, y * u))
        }
    }
}

/** The day and night line across the half map, fading toward the edges of the crop. */
internal fun DrawScope.drawTerminatorHalf(u: Float, shiftX: Float, sun: SunPosition, color: Color) {
    var prev: Offset? = null
    for (lonStep in -360..360) {
        val lon = lonStep.toDouble()
        val x = WorldProjection.x(lon) + shiftX
        val y = WorldProjection.y(Solar.terminatorLatitude(sun, lon)) + WorldHalfView.SHIFT_Y
        val p = Offset(x * u, y * u)
        val inside = x in 0f..WorldHalfView.WIDTH && y in 0f..WorldHalfView.HEIGHT
        val before = prev
        if (inside && before != null && before.x / u in 0f..WorldHalfView.WIDTH && before.y / u in 0f..WorldHalfView.HEIGHT) {
            val edge = minOf(x, y, WorldHalfView.WIDTH - x, WorldHalfView.HEIGHT - y)
            val alpha = (0.14f + 0.86f * min(1f, edge / 20f)).coerceIn(0.14f, 1f)
            drawLine(color.copy(alpha = alpha), before, p, AuraDockWorld.LineStroke * u, StrokeCap.Round)
        }
        prev = p
    }
}

/** A city dot with the stage's black as its outline; the home dot also gets a faint ring around it. */
internal fun DrawScope.drawHalfDot(x: Float, y: Float, radius: Float, color: Color, stage: Color, u: Float, ring: Color? = null) {
    if (ring != null) drawCircle(ring, 12f * u, Offset(x, y), style = Stroke(1f * u))
    drawCircle(color, radius, Offset(x, y))
    drawCircle(stage, radius, Offset(x, y), style = Stroke(AuraDockWorld.DotStroke * u))
}
