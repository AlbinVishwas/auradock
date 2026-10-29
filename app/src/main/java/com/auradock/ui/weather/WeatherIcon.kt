package com.auradock.ui.weather

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.auradock.weather.WeatherIconKind
import kotlin.math.cos
import kotlin.math.sin

/**
 * A simple, thin-lined weather icon drawn with code (no image files), in one color so it can be dimmed like the rest of
 * the dock. Thin strokes also keep it burn-in friendly.
 */
@Composable
fun WeatherIcon(kind: WeatherIconKind, color: Color, size: Dp = 32.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val stroke = Stroke(width = s * 0.07f, cap = StrokeCap.Round)
        when (kind) {
            WeatherIconKind.SUN -> sun(Offset(s / 2, s / 2), s * 0.2f, color, stroke)
            WeatherIconKind.PARTLY_CLOUDY -> {
                sun(Offset(s * 0.36f, s * 0.36f), s * 0.15f, color, stroke)
                cloud(Offset(s * 0.58f, s * 0.62f), s * 0.62f, color, stroke)
            }
            WeatherIconKind.CLOUD -> cloud(Offset(s / 2, s * 0.5f), s * 0.8f, color, stroke)
            WeatherIconKind.FOG -> {
                for ((i, w) in listOf(0.7f, 0.5f, 0.7f).withIndex()) {
                    val y = s * (0.3f + 0.2f * i)
                    drawLine(color, Offset(s * (0.5f - w / 2), y), Offset(s * (0.5f + w / 2), y), stroke.width, StrokeCap.Round)
                }
            }
            WeatherIconKind.DRIZZLE -> {
                cloud(Offset(s / 2, s * 0.38f), s * 0.74f, color, stroke)
                drops(s, color, stroke, count = 2)
            }
            WeatherIconKind.RAIN -> {
                cloud(Offset(s / 2, s * 0.38f), s * 0.74f, color, stroke)
                drops(s, color, stroke, count = 3)
            }
            WeatherIconKind.SNOW -> {
                cloud(Offset(s / 2, s * 0.38f), s * 0.74f, color, stroke)
                for (i in 0..2) drawCircle(color, s * 0.04f, Offset(s * (0.32f + 0.18f * i), s * (0.78f + if (i == 1) 0.06f else 0f)))
            }
            WeatherIconKind.STORM -> {
                cloud(Offset(s / 2, s * 0.36f), s * 0.74f, color, stroke)
                val bolt = Path().apply {
                    moveTo(s * 0.54f, s * 0.58f)
                    lineTo(s * 0.42f, s * 0.78f)
                    lineTo(s * 0.52f, s * 0.78f)
                    lineTo(s * 0.46f, s * 0.95f)
                    lineTo(s * 0.62f, s * 0.72f)
                    lineTo(s * 0.52f, s * 0.72f)
                    close()
                }
                drawPath(bolt, color, style = stroke)
            }
        }
    }
}

private fun DrawScope.sun(center: Offset, radius: Float, color: Color, stroke: Stroke) {
    drawCircle(color, radius, center, style = stroke)
    for (i in 0 until 8) {
        val angle = Math.toRadians(i * 45.0)
        val from = Offset(center.x + (radius * 1.5f) * cos(angle).toFloat(), center.y + (radius * 1.5f) * sin(angle).toFloat())
        val to = Offset(center.x + (radius * 1.95f) * cos(angle).toFloat(), center.y + (radius * 1.95f) * sin(angle).toFloat())
        drawLine(color, from, to, stroke.width, StrokeCap.Round)
    }
}

/** A cloud outline centered at [center], [width] wide: a flat base with three round bumps on top. */
private fun DrawScope.cloud(center: Offset, width: Float, color: Color, stroke: Stroke) {
    val w = width
    val baseY = center.y + w * 0.14f
    val path = Path().apply {
        moveTo(center.x - w * 0.36f, baseY)
        // left bump
        arcTo(androidx.compose.ui.geometry.Rect(center.x - w * 0.5f, baseY - w * 0.28f, center.x - w * 0.14f, baseY), 90f, 180f, false)
        // big middle bump
        arcTo(androidx.compose.ui.geometry.Rect(center.x - w * 0.28f, baseY - w * 0.5f, center.x + w * 0.14f, baseY - w * 0.08f), 180f, 190f, false)
        // right bump
        arcTo(androidx.compose.ui.geometry.Rect(center.x + w * 0.0f, baseY - w * 0.32f, center.x + w * 0.5f, baseY + w * 0.0f), 250f, 180f, false)
        lineTo(center.x - w * 0.36f, baseY)
    }
    drawPath(path, color, style = stroke)
}

private fun DrawScope.drops(s: Float, color: Color, stroke: Stroke, count: Int) {
    val xs = if (count == 2) listOf(0.4f, 0.6f) else listOf(0.33f, 0.5f, 0.67f)
    for ((i, x) in xs.withIndex()) {
        val y = s * (0.68f + if (i % 2 == 1) 0.06f else 0f)
        drawLine(color, Offset(s * x, y), Offset(s * (x - 0.05f), y + s * 0.14f), stroke.width, StrokeCap.Round)
    }
}
