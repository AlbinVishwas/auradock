package com.auradock.ui.widgets

import com.auradock.ui.theme.AuraDockMotion
import com.auradock.ui.theme.AuraDockSizes
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auradock.ui.theme.AuraDockColors
import com.auradock.ui.theme.AuraDockSpacing
import com.auradock.ui.theme.AuraDockTypography
import java.text.SimpleDateFormat
import java.util.*

/**
 * # ClockWidget  (Step 4 — enhanced rendering)
 *
 * Full-width ambient clock for the standby screen top section.
 *
 * ## Three-layer animation system
 *
 * | Layer | Technique | Purpose |
 * |-------|-----------|---------|
 * | Ghost glow | `Modifier.blur(AuraDockSizes.blurGlow)` on a duplicate `Text` | Soft ambient halo around digits |
 * | Colon blink | `infiniteRepeatable` step-function alpha | Rhythmic clock feel, 1 s period |
 * | Minute crossfade | `animateFloatAsState` 0→1 on minute change | Smooth digit swap without jitter |
 *
 * ## Typography
 * 96 sp · ExtraLight · letterSpacing −4 sp — maximises glanceability at 2 m.
 *
 * ## Layout
 * `Box` stacks glow ghost (blurred) under the sharp foreground Row.
 * The ghost is `pointer-events: none` equivalent (purely decorative).
 *
 * @param timeMillis  Live epoch millis — driven by `tickerFlow(1.seconds)` in ViewModel.
 * @param use24Hour   If true, shows 24-hour format; defaults to false (12h + AM/PM).
 * @param modifier    Caller-supplied size / position modifier.
 */
@Composable
fun ClockWidget(
    timeMillis : Long    = System.currentTimeMillis(),
    use24Hour  : Boolean = false,
    /** Low-contrast look for OLED night use: soft gray digits, no glow, nothing pure white. */
    dimmed     : Boolean = false,
    showDate   : Boolean = true,
    modifier   : Modifier = Modifier
) {
    val digitColor = if (dimmed) AuraDockColors.DimDigits else AuraDockColors.DockText
    val colonColor = if (dimmed) AuraDockColors.DimColon else AuraDockColors.DockLabel
    val labelColor = if (dimmed) AuraDockColors.DimLabel else AuraDockColors.DockSecondary
    // ── Time components (recomputed only when millis cross a minute boundary) ─
    val calendar = remember(timeMillis) {
        Calendar.getInstance().apply { timeInMillis = timeMillis }
    }

    val rawHour   = calendar.get(Calendar.HOUR_OF_DAY)
    val minute    = calendar.get(Calendar.MINUTE)

    val displayHour = if (use24Hour) rawHour else (rawHour % 12).let { if (it == 0) 12 else it }
    val hourStr     = remember(displayHour)   { "%02d".format(displayHour) }
    val minuteStr   = remember(timeMillis)    { "%02d".format(minute) }
    val amPmStr     = remember(rawHour)       { if (rawHour < 12) "AM" else "PM" }
    val dateStr     = remember(timeMillis) {
        SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date(timeMillis))
    }

    // ── Step 4a: Colon blink ──────────────────────────────────────────────────
    // Uses a step-function to snap between 0.85 and 0.25 opacity every second,
    // matching real clock behaviour without a smoothing curve.
    val colonAlpha by rememberInfiniteTransition(label = "colonBlink").animateFloat(
        initialValue  = 0.85f,
        targetValue   = 0.25f,
        animationSpec = infiniteRepeatable(
            animation  = tween(durationMillis = AuraDockMotion.ColonBlink, easing = StepEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "colonAlpha"
    )

    // ── Step 4b: Minute crossfade ─────────────────────────────────────────────
    // Targets 0f when minute changes, then snaps back to 1f.
    // The 0→1 tween is what the user sees as a smooth appear; the 1→0 is instant.
    var minuteAlphaTarget by remember { mutableFloatStateOf(1f) }
    val previousMinute    = remember { mutableIntStateOf(minute) }

    LaunchedEffect(minute) {
        if (minute != previousMinute.intValue) {
            previousMinute.intValue = minute
            minuteAlphaTarget = 0f
            // Snap to 0 immediately, then animate back to 1
        }
        minuteAlphaTarget = 1f
    }

    val minuteAlpha by animateFloatAsState(
        targetValue   = minuteAlphaTarget,
        animationSpec = tween(durationMillis = AuraDockMotion.MinuteSwap, easing = AuraDockMotion.Easing),
        label         = "minuteFade"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier         = modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AuraDockSpacing.xs)
        ) {

            // ── Step 4c: Stacked glow + sharp clock row ───────────────────────
            Box(contentAlignment = Alignment.Center) {

                // Sharp foreground clock row
                Row(verticalAlignment = Alignment.CenterVertically) {

                    // Hour
                    Text(
                        text  = hourStr,
                        style = AuraDockTypography.ClockDisplay.copy(color = digitColor)
                    )

                    // Blinking colon — cyan tint, step-function opacity
                    Text(
                        text  = ":",
                        style = AuraDockTypography.ClockDisplay.copy(
                            color = colonColor.copy(alpha = colonAlpha)
                        ),
                        modifier = Modifier.padding(horizontal = AuraDockSpacing.xxs)
                    )

                    // Minute — fades in on each tick
                    Text(
                        text  = minuteStr,
                        style = AuraDockTypography.ClockDisplay.copy(
                            color = digitColor.copy(alpha = minuteAlpha)
                        )
                    )

                    // AM / PM suffix — visible only in 12h mode
                    if (!use24Hour) {
                        Spacer(Modifier.width(AuraDockSpacing.sm))
                        Text(
                            text  = amPmStr,
                            style = AuraDockTypography.ClockMeridiem.copy(color = labelColor),
                            modifier = Modifier
                                .align(Alignment.Bottom)
                                .padding(bottom = AuraDockSpacing.md)
                        )
                    }
                }
            }

            // ── Date subtitle ─────────────────────────────────────────────────
            if (showDate) {
                Text(
                    text  = dateStr,
                    style = AuraDockTypography.ClockCaption.copy(color = labelColor)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Private easing — step function for colon blink (no interpolation between
// visible and invisible; mimics physical clock colon behaviour).
// ─────────────────────────────────────────────────────────────────────────────

private val StepEasing = Easing { fraction -> if (fraction < 1f) 0f else 1f }
