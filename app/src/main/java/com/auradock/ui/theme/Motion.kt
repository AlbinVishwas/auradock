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
    const val Check = 120
    const val Standard = 250
    const val Strike = 220

    /** The next-meeting line breathes: one slow fade out and back, in this many ms (opacity only, no movement). */
    const val MeetingPulse = 2400
    const val DockFade = 320
    const val MinuteSwap = 300
    const val Slow = 400
    const val ColonBlink = 500
    const val IllustrationFrame = 2200
    const val ClockDrift = 5200

    /** Blur to focus: how long new content takes to settle in. */
    const val BlurToFocus = 260

    /** Blur to focus in a list: the delay between one row and the next. */
    const val Stagger = 40

    /** A ticked task waits this long (so the tick is seen) before it eases down to the done rows: 600 ms. */
    const val TaskSink = 600

    /** How long "Undo focus" stays on the dock after a touch: four seconds. */
    const val UndoFocus = 4000

    /** How long the first-run dock preview shows before it moves on: about five seconds. */
    const val OnboardingPreview = 5000

    /** How long the undock summary card stays: three seconds (a tap closes it sooner). */
    const val UndockCard = 3000

    /** The Dock now morph (container transform from the pill into the dock). */
    const val DockMorph = 450

    /** The dock scales between this fraction of the screen (the size of its thumbnail) and full size when it opens and closes. */
    const val DockMorphScale = 0.35f

    /** Blur to focus: the blur radius content starts at, in dp. */
    const val BlurToFocusRadiusDp = 12

    /** Blur to focus: how far content rises while it settles, in dp. */
    const val BlurToFocusRiseDp = 4

    /** The scrim behind a sheet fades to this much black. */
    const val ScrimAlpha = 0.6f

    val Easing = FastOutSlowInEasing
    val EasingLinear = LinearEasing
    val EasingEmphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** A quick, even tween. */
    fun <T> tween(durationMillis: Int = Standard, easing: androidx.compose.animation.core.Easing = Easing): TweenSpec<T> =
        androidx.compose.animation.core.tween(durationMillis = durationMillis, easing = easing)

    /** The style guide's spring for sheets, cards and things that slide in (the docking pill): damping 0.9, stiffness 380, about 320 ms. No overshoot. */
    fun <T> softSpring(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.9f, stiffness = 380f)

    /** A calm spring without overshoot: things that settle into place. */
    fun <T> calmSpring(): FiniteAnimationSpec<T> = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)
}
