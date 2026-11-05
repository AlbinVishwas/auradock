package com.auradock.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

// The tokens live next to this file: Color.kt, Type.kt, Dimens.kt, Motion.kt. This file wires them into Material and provides the theme.

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

/** Material's defaults, with every style set in Inter so plain `Text` matches the rest of the UI. */
private val AuraDockMaterialTypography = Typography().let {
    Typography(
        displayLarge = it.displayLarge.copy(fontFamily = InterFamily), displayMedium = it.displayMedium.copy(fontFamily = InterFamily),
        displaySmall = it.displaySmall.copy(fontFamily = InterFamily), headlineLarge = it.headlineLarge.copy(fontFamily = InterFamily),
        headlineMedium = it.headlineMedium.copy(fontFamily = InterFamily), headlineSmall = it.headlineSmall.copy(fontFamily = InterFamily),
        titleLarge = it.titleLarge.copy(fontFamily = InterFamily), titleMedium = it.titleMedium.copy(fontFamily = InterFamily),
        titleSmall = it.titleSmall.copy(fontFamily = InterFamily), bodyLarge = it.bodyLarge.copy(fontFamily = InterFamily),
        bodyMedium = it.bodyMedium.copy(fontFamily = InterFamily), bodySmall = it.bodySmall.copy(fontFamily = InterFamily),
        labelLarge = it.labelLarge.copy(fontFamily = InterFamily), labelMedium = it.labelMedium.copy(fontFamily = InterFamily),
        labelSmall = it.labelSmall.copy(fontFamily = InterFamily)
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// COMPOSITION LOCAL — provides spacing across the tree without param drilling
// ─────────────────────────────────────────────────────────────────────────────

data class AuraDockSpacingValues(
    val hairline: Dp = AuraDockSpacing.hairline,
    val xxs: Dp = AuraDockSpacing.xxs,
    val xs: Dp  = AuraDockSpacing.xs,
    val sm: Dp  = AuraDockSpacing.sm,
    val smd: Dp = AuraDockSpacing.smd,
    val md: Dp  = AuraDockSpacing.md,
    val mdl: Dp = AuraDockSpacing.mdl,
    val lg: Dp  = AuraDockSpacing.lg,
    val xl: Dp  = AuraDockSpacing.xl,
    val xxl: Dp = AuraDockSpacing.xxl,
    val huge: Dp = AuraDockSpacing.huge,
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
            typography  = AuraDockMaterialTypography,
            content     = content
        )
    }
}
