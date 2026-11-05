package com.auradock.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/** How things move. Screens use these and never write a duration or spring themselves (see docs/DESIGN.md). */
object AuraDockMotion {
    // Durations, milliseconds.
    const val Press = 110
    const val PressRelease = 180
    const val Fast = 150
    const val ScreenOut = 200
    const val ScreenIn = 260
    const val Check = 220
    const val Standard = 250
    const val Strike = 280
    const val DockFade = 320
    const val MinuteSwap = 350
    const val Slow = 400
    const val ColonBlink = 500
    const val IllustrationFrame = 2200
    const val ClockDrift = 5200

    val Easing = FastOutSlowInEasing
    val EasingLinear = LinearEasing
    val EasingEmphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** A quick, even tween. */
    fun <T> tween(durationMillis: Int = Standard, easing: androidx.compose.animation.core.Easing = Easing): TweenSpec<T> =
        androidx.compose.animation.core.tween(durationMillis = durationMillis, easing = easing)

    /** The soft spring with a little overshoot: things that slide in (the docking pill). */
    fun <T> softSpring(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMediumLow)

    /** A calm spring without overshoot: things that settle into place. */
    fun <T> calmSpring(): FiniteAnimationSpec<T> = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)
}
