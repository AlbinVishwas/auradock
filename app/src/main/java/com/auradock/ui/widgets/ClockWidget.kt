package com.auradock.ui.widgets

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
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
 * | Ghost glow | `Modifier.blur(18.dp)` on a duplicate `Text` | Soft ambient halo around digits |
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
    val digitColor = if (dimmed) DimDigits else Color.White
    val colonColor = if (dimmed) DimColon else AuraDockColors.ClockGlow
    val labelColor = if (dimmed) DimLabel else AuraDockColors.OnSurfaceVariant
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
            animation  = tween(durationMillis = 500, easing = StepEasing),
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
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label         = "minuteFade"
    )

    val glowBreath by rememberInfiniteTransition(label = "clockGlowBreath").animateFloat(
        initialValue = 0.28f,
        targetValue = 0.42f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowBreathAlpha"
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

                // Ghost layer — blurred copy creates the ambient halo.
                // Drawn first so it sits visually behind the sharp text.
                if (!dimmed) {
                    Text(
                        text = "$hourStr:$minuteStr",
                        style = AuraDockTypography.ClockDisplay.copy(
                            color = AuraDockColors.ClockGlow.copy(
                                alpha = glowBreath * minuteAlpha
                            )
                        ),
                        modifier = Modifier.blur(20.dp)
                    )
                }

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
                        modifier = Modifier.padding(horizontal = 2.dp)
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
                            style = AuraDockTypography.Caption.copy(
                                color      = labelColor,
                                fontSize   = 18.sp,
                                fontWeight = FontWeight.Light
                            ),
                            modifier = Modifier
                                .align(Alignment.Bottom)
                                .padding(bottom = 16.dp)
                        )
                    }
                }
            }

            // ── Date subtitle ─────────────────────────────────────────────────
            if (showDate) {
                Text(
                    text  = dateStr,
                    style = AuraDockTypography.WidgetLabel.copy(
                        color         = labelColor,
                        letterSpacing = 2.sp,
                        fontWeight    = FontWeight.Light,
                        fontSize      = 13.sp
                    )
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Private easing — step function for colon blink (no interpolation between
// visible and invisible; mimics physical clock colon behaviour).
// ─────────────────────────────────────────────────────────────────────────────

// Dimmed (OLED night) palette: nothing is pure white.
private val DimDigits = Color(0xFF8A8A8A)
private val DimColon = Color(0xFF55616A)
private val DimLabel = Color(0xFF666666)

private val StepEasing = Easing { fraction -> if (fraction < 1f) 0f else 1f }
