package com.auradock.dock

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Density
import com.auradock.ui.theme.AuraDockBold
import com.auradock.ui.theme.AuraDockColors
import com.auradock.ui.theme.AuraDockDockLayout
import com.auradock.ui.theme.AuraDockSpacing
import com.auradock.ui.theme.NunitoFamily
import java.time.Instant
import java.time.ZoneId

/**
 * The time as the Bold template writes it: "12:30", "9:41", "10:08" on a 12-hour clock (no leading zero), "07:05" on a
 * 24-hour clock. [use24Hour] follows the phone's setting.
 */
fun boldTimeString(timeMillis: Long, use24Hour: Boolean, zone: ZoneId = ZoneId.systemDefault()): String {
    val t = Instant.ofEpochMilli(timeMillis).atZone(zone)
    val hour = if (use24Hour) t.hour else (t.hour % 12).let { if (it == 0) 12 else it }
    return (if (use24Hour) "%02d".format(hour) else hour.toString()) + ":" + "%02d".format(t.minute)
}

/**
 * Bold: nothing but the time, as big as the screen allows. Nunito Black digits at 80% opacity, alternating coral and light
 * coral, each leaning a little, tucked into each other and blended with "screen" where they overlap; the colon is two big
 * translucent grey dots. Pure black behind. [scale] makes it smaller (when the notification strip is showing).
 */
@Composable
fun BoldClock(timeMillis: Long, use24Hour: Boolean, modifier: Modifier = Modifier, scale: Float = 1f) {
    val text = remember(timeMillis, use24Hour) { boldTimeString(timeMillis, use24Hour) }
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    // Sizes are in pixels and ignore the phone's font-size setting: this is a picture of a time, not reading text.
    val fixed = remember(density) { Density(density.density, fontScale = 1f) }
    val style = TextStyle(
        fontFamily = NunitoFamily,
        fontWeight = FontWeight.Black,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both)
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = AuraDockDockLayout.screenPadding, vertical = AuraDockSpacing.lg),
        contentAlignment = Alignment.Center
    ) {
        val availW = constraints.maxWidth.toFloat()
        val availH = constraints.maxHeight.toFloat()
        val refPx = 200f
        val refStyle = style.copy(fontSize = with(fixed) { refPx.toSp() })
        // How wide each glyph is, in em (a colon is a fixed slot).
        val widthsEm = text.map { c ->
            if (c == ':') AuraDockBold.ColonEm else measurer.measure(c.toString(), refStyle).size.width / refPx
        }
        val gaps = text.indices.drop(1).count { text[it] != ':' && text[it - 1] != ':' }
        val totalEm = widthsEm.sum() - AuraDockBold.OverlapEm * gaps + 2 * AuraDockBold.EdgeEm
        val emPx = minOf(availW * AuraDockBold.WidthFill / totalEm, availH / AuraDockBold.DigitHeightEm) * scale
        val fontSize = with(fixed) { emPx.toSp() }

        Row(
            // The padding sits inside the layer, so the leaning digits are not clipped at its edge.
            modifier = Modifier
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .padding(horizontal = with(fixed) { (AuraDockBold.EdgeEm * emPx).toDp() }),
            verticalAlignment = Alignment.CenterVertically
        ) {
            var digitIndex = 0
            text.forEachIndexed { i, c ->
                val tuckedIn = i > 0 && c != ':' && text[i - 1] != ':'
                val startShift = if (tuckedIn) -AuraDockBold.OverlapEm * emPx else 0f
                if (c == ':') {
                    val w = AuraDockBold.ColonEm * emPx
                    Canvas(Modifier.size(with(fixed) { w.toDp() }, with(fixed) { emPx.toDp() })) {
                        val r = AuraDockBold.ColonDotRadiusEm * emPx
                        val dy = AuraDockBold.ColonDotOffsetEm * emPx
                        drawCircle(AuraDockColors.BoldColon, r, Offset(size.width / 2, size.height / 2 - dy))
                        drawCircle(AuraDockColors.BoldColon, r, Offset(size.width / 2, size.height / 2 + dy))
                    }
                } else {
                    val color = (if (digitIndex % 2 == 0) AuraDockColors.Coral else AuraDockColors.CoralLight).copy(alpha = AuraDockBold.DigitAlpha)
                    val degrees = AuraDockBold.rotationDegrees(c)
                    digitIndex++
                    Text(
                        text = c.toString(),
                        style = style.copy(fontSize = fontSize, color = color),
                        softWrap = false,
                        modifier = Modifier
                            .layout { measurable, constraints ->
                                val p = measurable.measure(constraints)
                                layout(p.width + startShift.toInt(), p.height) { p.place(startShift.toInt(), 0) }
                            }
                            .drawWithContent {
                                // Screen blend with what the earlier digits drew, and a lean about the digit's own centre.
                                drawIntoCanvas { canvas ->
                                    val paint = Paint().apply { blendMode = BlendMode.Screen }
                                    canvas.saveLayer(Rect(-size.width, -size.height, size.width * 2, size.height * 2), paint)
                                    rotate(degrees) { this@drawWithContent.drawContent() }
                                    canvas.restore()
                                }
                            }
                    )
                }
            }
        }
    }
}
