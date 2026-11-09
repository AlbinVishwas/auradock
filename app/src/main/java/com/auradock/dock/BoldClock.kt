package com.auradock.dock

import android.graphics.Paint as NativePaint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect as NativeRect
import android.graphics.Typeface
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.auradock.ui.theme.AuraDockBold
import com.auradock.ui.theme.AuraDockColors
import com.auradock.ui.theme.AuraDockDockLayout
import com.auradock.ui.theme.AuraDockMotion
import com.auradock.ui.theme.AuraDockTypography
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

/** Digits are measured at this size (px) to read their ink; the numbers are then divided back to em. */
private const val MEASURE_PX = 1000f

/** The ink of each digit in [typeface], in em. */
private fun measureInks(typeface: Typeface): Map<Char, Ink> {
    val paint = NativePaint().apply { this.typeface = typeface; textSize = MEASURE_PX }
    val r = NativeRect()
    return ('0'..'9').associateWith { c ->
        paint.getTextBounds(c.toString(), 0, 1, r)
        Ink(r.left / MEASURE_PX, r.top / MEASURE_PX, r.right / MEASURE_PX, r.bottom / MEASURE_PX)
    }
}

/**
 * Bold: nothing but the time, as big as the safe area allows. Nunito Black digits, upright, alternating coral and light coral,
 * each placed by its ink so every pair overlaps by the same amount; the colon is two dots in the gap. The whole time is laid out
 * in landscape coordinates (the dock turns the finished layer when the window is portrait), its combined ink box centred in the
 * safe area, or in the area above the notification strip when there is one. Pure black behind.
 */
@Composable
fun BoldClock(timeMillis: Long, use24Hour: Boolean, modifier: Modifier = Modifier, strip: List<String> = emptyList(), caption: String? = null) {
    val stripShown = strip.isNotEmpty() || caption != null
    val text = remember(timeMillis, use24Hour) { boldTimeString(timeMillis, use24Hour) }
    val resolver = LocalFontFamilyResolver.current
    val typeface = remember(resolver) { resolver.resolve(NunitoFamily, FontWeight.Black).value as Typeface }
    val inks = remember(typeface) { measureInks(typeface) }

    Column(modifier = modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        // The minute changes with a 300 ms crossfade: the old time fades out as the new one fades in.
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Crossfade(targetState = text, animationSpec = tween(AuraDockMotion.MinuteSwap), label = "boldMinute") { shown ->
                BoldDigits(shown, typeface, inks, Modifier.fillMaxSize())
            }
        }
        if (stripShown) {
            Column(
                modifier = Modifier.padding(top = AuraDockBold.StripGap, bottom = AuraDockBold.SafeVertical),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AuraDockBold.StripLineGap)
            ) {
                // The held-for-you counts sit next to the last notification line (alone when there is none), a step quieter.
                strip.forEachIndexed { i, line -> BoldStripLine(line, if (i == strip.lastIndex) caption else null) }
                if (strip.isEmpty() && caption != null) Text(text = caption, color = AuraDockColors.HeldCaption, style = AuraDockTypography.BoldStrip, maxLines = 1)
            }
        }
    }
}

/** The time, drawn by ink into whatever area it is given. */
@Composable
private fun BoldDigits(text: String, typeface: Typeface, inks: Map<Char, Ink>, modifier: Modifier) {
    val plan = remember(text, inks) { planBold(text) { inks.getValue(it) } }
    Canvas(modifier = modifier.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
        val safeW = size.width - 2 * AuraDockBold.SafeSide.toPx()
        val safeH = size.height - 2 * AuraDockBold.SafeVertical.toPx()
        val em = boldFontSizePx(plan, safeW, safeH)
        if (em <= 0f) return@Canvas
        // The combined ink box of the digits and the colon is centred on the area's centre.
        val originX = size.width / 2f - plan.centerX * em
        val originY = size.height / 2f - plan.centerY * em
        val paint = NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textSize = em
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
        }
        drawIntoCanvas { canvas ->
            plan.digits.forEachIndexed { i, d ->
                val tone = if (i % 2 == 0) AuraDockColors.Coral else AuraDockColors.CoralLight
                paint.color = tone.copy(alpha = AuraDockBold.DigitAlpha).toArgb()
                canvas.nativeCanvas.drawText(d.char.toString(), originX + d.penX * em, originY, paint)
            }
        }
        // The colon is drawn last, above the digits, with the same screen blend.
        plan.colonX?.let { x ->
            val r = AuraDockBold.ColonDiameterEm * em / 2f
            paint.color = AuraDockColors.BoldColon.toArgb()
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawCircle(originX + x * em, originY + plan.colonUpperY * em, r, paint)
                canvas.nativeCanvas.drawCircle(originX + x * em, originY + plan.colonLowerY * em, r, paint)
            }
        }
    }
}

/** One notification under the Bold clock: a chat-bubble icon and "App · who", Inter Medium 16, grey. */
@Composable
private fun BoldStripLine(text: String, caption: String? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AuraDockBold.StripIconGap)) {
        Icon(
            imageVector = Icons.Outlined.ChatBubbleOutline,
            contentDescription = null,
            tint = AuraDockColors.DockSecondary,
            modifier = Modifier.size(AuraDockBold.StripIcon)
        )
        Text(
            text = text,
            color = AuraDockColors.DockSecondary,
            style = AuraDockTypography.BoldStrip,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        if (caption != null) {
            Text(text = caption, color = AuraDockColors.HeldCaption, style = AuraDockTypography.BoldStrip, maxLines = 1, modifier = Modifier.padding(start = AuraDockDockLayout.captionGap))
        }
    }
}
