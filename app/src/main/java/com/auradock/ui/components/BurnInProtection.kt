package com.auradock.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * # BurnInProtection  (Step 5)
 *
 * Applies a slow, barely-perceptible XY drift to any composable to prevent
 * static pixel burn-in on AMOLED displays during extended standby periods.
 *
 * ## Why this matters
 * In standby mode the display remains on for hours with a nearly static image.
 * OLED pixels degrade unevenly when the same sub-pixels are lit continuously.
 * A ±4 dp drift over a 10-minute cycle shifts every lit pixel by ~12 physical
 * pixels on a 3x-density display — enough to distribute wear without the user
 * noticing any movement.
 *
 * ## Animation approach
 * Two independent `infiniteRepeatable` tweens drive X and Y with a deliberate
 * phase offset (X: 0 ms delay, Y: 2.5 min offset). Using sine-like
 * [FastOutSlowInEasing] produces organic, non-mechanical motion with no
 * abrupt position jumps.
 *
 * ```
 *  X drift:  0 dp ──(5 min)──►  +4 dp ──(5 min)──►  0 dp  …
 *  Y drift:  0 dp ──(2.5 min)── ► +4 dp ──(5 min)──► -4 dp ─► 0 dp …
 * ```
 *
 * ## Usage
 * Wrap the root content composable — not individual widgets — so the entire
 * screen contents shift together, preserving relative layout integrity.
 *
 * ```kotlin
 * BurnInProtection {
 *     StandbyContent(...)
 * }
 * ```
 *
 * @param rangeDP   Maximum displacement in each direction (default ±4 dp).
 * @param periodMs  Full cycle duration in milliseconds (default 10 min = 600 000 ms).
 * @param modifier  Caller-supplied modifier (applied outside the drift offset).
 * @param content   Composable subtree to protect.
 */
@Composable
fun BurnInProtection(
    rangeDP  : Float    = 4f,
    periodMs : Int      = 600_000,    // 10 minutes
    modifier : Modifier = Modifier,
    content  : @Composable (Modifier) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "burnInDrift")

    // ── X-axis drift (full period, no initial delay) ───────────────────────
    val driftX by infiniteTransition.animateFloat(
        initialValue  = 0f,
        targetValue   = rangeDP,
        animationSpec = infiniteRepeatable(
            animation  = tween(
                durationMillis = periodMs / 2,
                easing         = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "driftX"
    )

    // ── Y-axis drift (offset phase = period/4 for non-linear path) ─────────
    // Using a shorter half-period cycle that is phase-offset from X gives the
    // overall path a subtle Lissajous-like organic feel rather than a
    // mechanical back-and-forth on one axis.
    val driftY by infiniteTransition.animateFloat(
        initialValue  = 0f,
        targetValue   = -rangeDP,
        animationSpec = infiniteRepeatable(
            animation  = tween(
                durationMillis = (periodMs * 0.75f).toInt(),  // phase offset
                easing         = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "driftY"
    )

    content(
        modifier.offset(
            x = driftX.dp,
            y = driftY.dp
        )
    )
}
