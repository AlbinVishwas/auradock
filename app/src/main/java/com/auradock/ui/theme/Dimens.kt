package com.auradock.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Space between and around things. Nothing outside the theme writes a `.dp` literal for spacing (see docs/DESIGN.md). */
object AuraDockSpacing {
    val hairline: Dp = 1.dp
    val xxs: Dp = 2.dp
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val smd: Dp = 12.dp
    val md: Dp = 16.dp
    val mdl: Dp = 20.dp
    val lg: Dp = 24.dp
    val xl: Dp = 32.dp
    val xxl: Dp = 48.dp
    val huge: Dp = 64.dp

    /** Internal padding of a widget card. */
    val CardPadding: Dp = 20.dp

    /** Gap between the two widget cards in the bottom row. */
    val WidgetGap: Dp = 16.dp
}

/** Rounded corners. */
object AuraDockRadii {
    val control: Dp = 12.dp
    val card: Dp = 16.dp
    val cardLarge: Dp = 24.dp
    val widget: Dp = 28.dp
    val pill: Dp = 999.dp
}

/** Fixed sizes of things: icons, touch targets, line widths. */
object AuraDockSizes {
    val none: Dp = 0.dp
    val line: Dp = 1.dp
    val strikeLine: Dp = 1.5.dp
    val lineThick: Dp = 2.dp
    val iconSmall: Dp = 18.dp
    val icon: Dp = 24.dp
    val weatherIconSmall: Dp = 22.dp
    val weatherIcon: Dp = 32.dp
    val appIcon: Dp = 40.dp
    val touchTarget: Dp = 44.dp
    val button: Dp = 48.dp
    val blurGlow: Dp = 20.dp
    val elevationCard: Dp = 16.dp
    val elevationPill: Dp = 8.dp
    /** How far above the bottom edge of the screen the docking pill floats, in dp. */
    const val pillBottomOffsetDp = 72
}

/** Layout numbers that belong to the dock templates (landscape, seen from a distance). */
object AuraDockDockLayout {
    val contentMaxWidth: Dp = 560.dp
    val timeColumn: Dp = 104.dp
    val labelColumn: Dp = 92.dp
    val screenPadding: Dp = 40.dp
    val columnGap: Dp = 28.dp
    val sectionGap: Dp = 36.dp
    val listGap: Dp = 18.dp
    val cardPadding: Dp = 32.dp
    val cardGap: Dp = 20.dp
    val stripPadding: Dp = 48.dp
    val weatherIcon: Dp = 36.dp
    val weatherIconLarge: Dp = 44.dp
    val taskRing: Dp = 14.dp
    val taskRingGap: Dp = 14.dp
    val strokeRing: Dp = 1.5.dp
    val weatherGap: Dp = 10.dp
    val stripBase: Dp = 24.dp
    val stripLine: Dp = 20.dp
    val stripVertical: Dp = 20.dp
}

/** The onboarding animation's drawing sizes. */
object AuraDockIllustration {
    val height: Dp = 150.dp
    val phoneWidth: Dp = 200.dp
    val phoneHeight: Dp = 96.dp
    val top: Dp = 18.dp
    val ring: Dp = 40.dp
    val ringStroke: Dp = 3.dp
    val phoneRadius: Dp = 14.dp
    val screenRadius: Dp = 10.dp
    val bezel: Dp = 4.dp
    val standGap: Dp = 4.dp
    val standHeight: Dp = 8.dp
    val standCorner: Dp = 4.dp
    val appLineInset: Dp = 14.dp
    val appLineStep: Dp = 18.dp
    val appLineHeight: Dp = 8.dp
    val appLineWidth: Dp = 110.dp
    val appLineShrink: Dp = 25.dp
}

/** Sizes in the template gallery. */
object AuraDockGallery {
    val pageInset: Dp = 64.dp
    val previewDesignWidth: Dp = 800.dp
    val cardPadding: Dp = 18.dp
}
