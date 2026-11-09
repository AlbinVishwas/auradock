package com.auradock.dock.world

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.auradock.dock.BoldStripLine
import com.auradock.dock.boldTimeString
import com.auradock.ui.theme.AuraDockColors
import com.auradock.ui.theme.AuraDockTypography
import com.auradock.ui.theme.AuraDockWorld
import java.time.Instant
import java.time.ZoneId
import kotlin.math.roundToInt

/** What the World dock needs besides the time: where home is (null: no location) and the extra cities picked. */
data class WorldUiState(val home: GeoPoint? = null, val extras: List<WorldCity> = emptyList())

/** A city's local time as the map writes it: "12:43 am" on a 12-hour clock, "00:43" on a 24-hour one. */
fun worldCityTime(millis: Long, zoneId: String, use24Hour: Boolean): String {
    val t = Instant.ofEpochMilli(millis).atZone(ZoneId.of(zoneId))
    if (use24Hour) return "%02d:%02d".format(t.hour, t.minute)
    val hour = (t.hour % 12).let { if (it == 0) 12 else it }
    return "%d:%02d %s".format(hour, t.minute, if (t.hour < 12) "am" else "pm")
}

/**
 * World: a dotted map of the earth with the day and night line, lit land in daylight, the home city's name and time bottom left
 * (the home dot in coral when the weather location is known), and up to two more cities as small labelled dots. The day and night line is
 * calculated from the real date and time every five minutes ([AuraDockWorld.RefreshMillis]), not every frame.
 */
@Composable
fun WorldClock(
    timeMillis: Long,
    use24Hour: Boolean,
    homeName: String?,
    state: WorldUiState,
    modifier: Modifier = Modifier,
    strip: List<String> = emptyList(),
    caption: String? = null,
    zone: ZoneId = ZoneId.systemDefault()
) {
    val stripShown = strip.isNotEmpty() || caption != null
    val fallback = remember(zone, timeMillis / 3_600_000L) { WorldCities.forZone(zone, timeMillis) }
    val homePoint = state.home ?: fallback.point
    val homeLabel = homeName?.takeIf { it.isNotBlank() } ?: fallback.name
    val bucket = timeMillis / AuraDockWorld.RefreshMillis
    val sun = remember(bucket) { Solar.at(bucket * AuraDockWorld.RefreshMillis) }
    val night = !Solar.isDay(sun, homePoint.lat, homePoint.lon)
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        val s = widthPx / WorldProjection.DESIGN_WIDTH // pixels per design unit
        val offsetY = (heightPx - WorldProjection.DESIGN_HEIGHT * s) / 2f

        val cityStyle = if (stripShown) AuraDockTypography.DockWorldCityCompact else AuraDockTypography.DockWorldCity
        val timeStyle = if (stripShown) AuraDockTypography.DockWorldTimeCompact else AuraDockTypography.DockWorldTime
        val homeTime = remember(timeMillis, use24Hour) { boldTimeString(timeMillis, use24Hour) }

        // The home label's box, so city labels keep clear of it.
        val cityPx = measurer.measure(homeLabel, cityStyle, maxLines = 1)
        val timePx = measurer.measure(homeTime, timeStyle, maxLines = 1)
        val gapPx = with(density) { (if (stripShown) AuraDockWorld.LabelGapNotification else AuraDockWorld.LabelGap).toPx() }
        val sidePx = with(density) { AuraDockWorld.SideInset.toPx() }
        val bottomPx = with(density) { (if (stripShown) AuraDockWorld.BottomInsetNotification else AuraDockWorld.BottomInset).toPx() }
        val homeBlockW = maxOf(cityPx.size.width, timePx.size.width).toFloat()
        // The time's line box is 0.8 em, its baseline a little above the bottom (as the frame's CSS line box puts it), whatever the font's own metrics.
        val timeBoxPx = with(density) { timeStyle.fontSize.toPx() } * AuraDockWorld.TimeLineEm
        val homeBlockH = cityPx.size.height + gapPx + timeBoxPx
        val homeBox = Box(sidePx, heightPx - bottomPx - homeBlockH, sidePx + homeBlockW, heightPx - bottomPx)

        // City labels, placed so none overlaps another, a dot or the home label.
        val padH = with(density) { AuraDockWorld.PillPadH.toPx() }
        val padV = with(density) { AuraDockWorld.PillPadV.toPx() }
        val pillGap = with(density) { AuraDockWorld.PillGap.toPx() }
        val pills = state.extras.map { c ->
            val label = "${c.short} ·"
            val time = worldCityTime(timeMillis, c.zoneId, use24Hour)
            val l = measurer.measure(label, AuraDockTypography.DockWorldPillLabel, maxLines = 1)
            val t = measurer.measure(time, AuraDockTypography.DockWorldPillTime, maxLines = 1)
            Triple(c, label to time, Pair(l.size.width + pillGap + t.size.width + 2 * padH, maxOf(l.size.height, t.size.height) + 2 * padV))
        }
        val requests = pills.mapIndexed { i, (c, _, size) ->
            LabelRequest("$i", WorldProjection.x(c.lon) * s, WorldProjection.y(c.lat) * s + offsetY, size.first, size.second)
        }
        val placed = remember(requests, homeBox, widthPx, heightPx) {
            placeLabels(requests, listOf(homeBox), Box(0f, 0f, widthPx, heightPx), dotRadius = AuraDockWorld.CityDotRadius * s + 2f)
        }

        val dayColor = AuraDockColors.WorldDay
        val nightColor = AuraDockColors.WorldNight
        val lineColor = AuraDockColors.WorldLine
        val homeColor = AuraDockColors.Coral
        val stage = AuraDockColors.Background
        val showHomeDot = state.home != null

        Canvas(Modifier.fillMaxSize()) {
            drawLand(s, offsetY, sun, dayColor, nightColor)
            drawTerminator(s, offsetY, sun, lineColor)
            state.extras.forEach { c ->
                drawDot(WorldProjection.x(c.lon) * s, WorldProjection.y(c.lat) * s + offsetY, AuraDockWorld.CityDotRadius * s, androidx.compose.ui.graphics.Color.White, stage, s)
            }
            if (showHomeDot) drawDot(WorldProjection.x(state.home!!.lon) * s, WorldProjection.y(state.home.lat) * s + offsetY, AuraDockWorld.HomeDotRadius * s, homeColor, stage, s)
        }

        placed.forEachIndexed { i, p ->
            val (_, texts, _) = pills[i]
            Row(
                modifier = Modifier
                    .offset { IntOffset(p.box.left.roundToInt(), p.box.top.roundToInt()) }
                    .background(stage, RoundedCornerShape(AuraDockWorld.PillRadius))
                    .padding(horizontal = AuraDockWorld.PillPadH, vertical = AuraDockWorld.PillPadV),
                horizontalArrangement = Arrangement.spacedBy(AuraDockWorld.PillGap),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(texts.first, color = AuraDockColors.DockSecondary, style = AuraDockTypography.DockWorldPillLabel, maxLines = 1)
                Text(texts.second, color = AuraDockColors.DockText, style = AuraDockTypography.DockWorldPillTime, maxLines = 1)
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = AuraDockWorld.SideInset, bottom = if (stripShown) AuraDockWorld.BottomInsetNotification else AuraDockWorld.BottomInset),
            verticalArrangement = Arrangement.spacedBy(if (stripShown) AuraDockWorld.LabelGapNotification else AuraDockWorld.LabelGap)
        ) {
            Text(
                text = homeLabel, style = cityStyle, color = AuraDockColors.Coral, maxLines = 1,
                modifier = if (night) Modifier.alpha(AuraDockWorld.NightCityAlpha) else Modifier
            )
            Text(
                text = homeTime, style = timeStyle, color = if (night) AuraDockColors.DockSecondary else AuraDockColors.DockText, maxLines = 1,
                modifier = Modifier.layout { measurable, c ->
                    val placeable = measurable.measure(c)
                    val fontPx = timeStyle.fontSize.toPx()
                    val box = (fontPx * AuraDockWorld.TimeLineEm).roundToInt()
                    val baseline = placeable[LastBaseline]
                    layout(placeable.width, box) { placeable.place(0, box - (fontPx * AuraDockWorld.TimeBaselineEm).roundToInt() - baseline) }
                }
            )
        }

        if (stripShown) {
            Column(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = AuraDockWorld.BottomInset),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                strip.forEachIndexed { i, line -> BoldStripLine(line, if (i == strip.lastIndex) caption else null) }
                if (strip.isEmpty() && caption != null) Text(text = caption, color = AuraDockColors.HeldCaption, style = AuraDockTypography.BoldStrip, maxLines = 1)
            }
        }
    }
}

/** The dotted land: lit where the sun is up at that dot, dark where it is not. */
private fun DrawScope.drawLand(s: Float, offsetY: Float, sun: SunPosition, day: androidx.compose.ui.graphics.Color, night: androidx.compose.ui.graphics.Color) {
    for (row in 0 until LandMask.ROWS) for (column in 0 until LandMask.COLUMNS) {
        if (!LandMask.isLand(column, row)) continue
        val x = WorldProjection.GRID_X0 + WorldProjection.GRID * column
        val y = WorldProjection.GRID_Y0 + WorldProjection.GRID * row
        val lit = Solar.isDay(sun, WorldProjection.latOfY(y), WorldProjection.lonOfX(x))
        drawCircle(if (lit) day else night, AuraDockWorld.LandDotRadius * s, Offset(x * s, y * s + offsetY))
    }
}

/** The day and night line, a fading 2 px curve sampled every degree of longitude (clipped to the frame's map area). */
private fun DrawScope.drawTerminator(s: Float, offsetY: Float, sun: SunPosition, color: androidx.compose.ui.graphics.Color) {
    var prev: Offset? = null
    var prevY = 0f
    for (lonStep in -180..180) {
        val lon = lonStep.toDouble()
        val y = WorldProjection.y(Solar.terminatorLatitude(sun, lon))
        val point = Offset(WorldProjection.x(lon) * s, y * s + offsetY)
        val p = prev
        if (p != null && y <= AuraDockWorld.LineClip && prevY <= AuraDockWorld.LineClip) {
            val alpha = when {
                y <= AuraDockWorld.LineFadeStart -> 1f
                else -> 1f - (1f - AuraDockWorld.LineMinAlpha) * ((y - AuraDockWorld.LineFadeStart) / (AuraDockWorld.LineFadeEnd - AuraDockWorld.LineFadeStart)).coerceIn(0f, 1f)
            }
            drawLine(color.copy(alpha = alpha), p, point, strokeWidth = AuraDockWorld.LineStroke * s, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        }
        prev = point
        prevY = y
    }
}

/** A city dot: a filled circle with the stage's black as its outline. */
private fun DrawScope.drawDot(x: Float, y: Float, radius: Float, color: androidx.compose.ui.graphics.Color, stage: androidx.compose.ui.graphics.Color, s: Float) {
    drawCircle(color, radius, Offset(x, y))
    drawCircle(stage, radius, Offset(x, y), style = Stroke(AuraDockWorld.DotStroke * s))
}
