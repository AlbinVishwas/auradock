package com.auradock.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────────────────────────────────────
// COLOR PALETTE
// ─────────────────────────────────────────────────────────────────────────────

object AuraDockColors {
    /** Pure ambient black — the stage everything sits on. */
    val Background       = Color(0xFF000000)

    /** Deep surface for widget cards (glassmorphic layer). */
    val Surface          = Color(0xFF0D0D0D)

    /** Glass card fill (top stop). */
    val GlassCardTop     = Color(0x17FFFFFF)   // white 9%

    /** Glass card fill (bottom stop). */
    val GlassCardBottom  = Color(0x0AFFFFFF)   // white 4%

    /** White border hairline on cards. */
    val GlassBorder      = Color(0x1FFFFFFF)   // white 12%

    /** Primary text — high contrast white. */
    val OnBackground     = Color(0xFFFFFFFF)

    /** Secondary/caption text — muted gray. */
    val OnSurfaceVariant = Color(0xFFAAAAAA)

    /** Clock glow accent — cool cyan-blue to match Samsung-style vibrancy. */
    val ClockGlow        = Color(0xFF4FC3F7)

    /** Today's date highlight ring on calendar. */
    val AccentBlue       = Color(0xFF2979FF)

    /** Weather icon warm accent. */
    val AccentAmber      = Color(0xFFFFB300)

    /** Subtle separator line. */
    val Divider          = Color(0x26FFFFFF)   // white 15%
}

// ─────────────────────────────────────────────────────────────────────────────
// TYPOGRAPHY SCALE
// ─────────────────────────────────────────────────────────────────────────────

/**
 * AuraDock uses the system default sans-serif (Roboto on Android) with
 * carefully chosen weights. If the app bundles "Inter" or "Plus Jakarta Sans",
 * swap [Default] here — no other changes needed.
 */
object AuraDockTypography {
    /** 96 sp · ExtraLight — main clock digits */
    val ClockDisplay = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.ExtraLight,
        fontSize   = 96.sp,
        letterSpacing = (-4).sp
    )

    /** 28 sp · Light — temperature / primary widget value */
    val WidgetPrimary = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Light,
        fontSize   = 28.sp,
        letterSpacing = (-0.5).sp
    )

    /** 14 sp · Medium — widget labels, city name */
    val WidgetLabel = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize   = 14.sp,
        letterSpacing = 0.5.sp
    )

    /** 12 sp · Regular — calendar day cells */
    val CalendarCell = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize   = 12.sp,
        letterSpacing = 0.sp
    )

    /** 10 sp · Medium — caption / secondary info */
    val Caption = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize   = 10.sp,
        letterSpacing = 1.sp
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// SPACING SYSTEM
// ─────────────────────────────────────────────────────────────────────────────

object AuraDockSpacing {
    val xs  : Dp = 4.dp
    val sm  : Dp = 8.dp
    val md  : Dp = 16.dp
    val lg  : Dp = 24.dp
    val xl  : Dp = 32.dp
    val xxl : Dp = 48.dp

    /** Corner radius for all widget cards. */
    val CardRadius: Dp = 28.dp

    /** Standard widget card internal padding. */
    val CardPadding: Dp = 20.dp

    /** Gap between the two widget cards in the bottom row. */
    val WidgetGap: Dp = 16.dp
}

// ─────────────────────────────────────────────────────────────────────────────
// MATERIAL 3 DARK SCHEME — wired to AuraDock palette
// ─────────────────────────────────────────────────────────────────────────────

private val AuraDarkColorScheme = darkColorScheme(
    background       = AuraDockColors.Background,
    surface          = AuraDockColors.Surface,
    onBackground     = AuraDockColors.OnBackground,
    onSurface        = AuraDockColors.OnBackground,
    onSurfaceVariant = AuraDockColors.OnSurfaceVariant,
    primary          = AuraDockColors.AccentBlue,
    secondary        = AuraDockColors.ClockGlow,
    surfaceTint      = Color.Transparent,
    scrim            = Color.Black
)

// ─────────────────────────────────────────────────────────────────────────────
// COMPOSITION LOCAL — provides spacing across the tree without param drilling
// ─────────────────────────────────────────────────────────────────────────────

data class AuraDockSpacingValues(
    val xs: Dp  = AuraDockSpacing.xs,
    val sm: Dp  = AuraDockSpacing.sm,
    val md: Dp  = AuraDockSpacing.md,
    val lg: Dp  = AuraDockSpacing.lg,
    val xl: Dp  = AuraDockSpacing.xl,
    val xxl: Dp = AuraDockSpacing.xxl,
)

val LocalAuraDockSpacing = staticCompositionLocalOf { AuraDockSpacingValues() }

// ─────────────────────────────────────────────────────────────────────────────
// THEME ENTRY POINT
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AuraDockTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalAuraDockSpacing provides AuraDockSpacingValues()) {
        MaterialTheme(
            colorScheme = AuraDarkColorScheme,
            content     = content
        )
    }
}
