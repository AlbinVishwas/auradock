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
    /** The margin at the left and right of every screen: 20 dp. */
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
    /** Cards: 24 dp. */
    val card: Dp = 24.dp
    val cardLarge: Dp = 24.dp
    val widget: Dp = 28.dp
    /** The top corners of a bottom sheet: 28 dp. */
    val sheet: Dp = 28.dp
    /** An app icon's rounded tile: 28 dp across, with this corner. */
    val tile: Dp = 8.dp
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
    /** The smallest thing you can tap: 48 dp. */
    val touchTarget: Dp = 48.dp
    val button: Dp = 48.dp
    /** The primary pill: 56 dp high. One per screen. */
    val pill: Dp = 56.dp
    /** An app icon's tile: 28 dp. */
    val appTile: Dp = 28.dp
    /** Toggle: track and thumb. */
    val toggleWidth: Dp = 52.dp
    val toggleHeight: Dp = 32.dp
    val toggleThumb: Dp = 24.dp
    /** Checkbox. */
    val checkbox: Dp = 24.dp
    /** The quiet pill's visible height; its tap area is still [touchTarget]. */
    val quietPill: Dp = 40.dp
    /** A progress dot, and the wider dot of the current step. */
    val dot: Dp = 8.dp
    val dotCurrent: Dp = 24.dp
    /** The icon disc on a permission card. */
    val iconDisc: Dp = 40.dp
    val checkStroke: Dp = 2.dp
    /** The logo mark on the welcome step. */
    val wordmarkMark: Dp = 96.dp
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
    /** The safe zone of every dock template: 40 dp at the sides and 24 dp at the top and bottom (the burn-in drift is on top of that). */
    val screenPadding: Dp = 40.dp
    val safeVertical: Dp = 24.dp
    /** The Focus digits: a digit is this fraction of the font size tall, and the two lines are this far apart. */
    const val FocusCapEm = 0.83f
    const val FocusGapEm = 0.10f
    /** The Classic day number's line height as a fraction of its size. */
    const val DayLineEm = 0.95f
    const val FocusLeftShare = 0.46f
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
    val pageInset: Dp = 40.dp
    val previewDesignWidth: Dp = 800.dp
    val cardPadding: Dp = 18.dp
}

/** The Bold template's clock (docs/design/STYLE_GUIDE.md): numbers in "em", meaning a fraction of the digit size. */
object AuraDockBold {
    /** Digits are drawn at 80% opacity, so overlapping ones screen into a lighter tone. */
    const val DigitAlpha = 0.8f

    /** How far each digit tucks into the one before it: -0.12 em. */
    const val OverlapEm = 0.12f

    /** The width of the colon's slot, and the size and spacing of its two dots. */
    /** Room kept at each end of the time so a leaning digit is not clipped. */
    const val EdgeEm = 0.08f

    /** Colon (Dock-Bold.dc.html): dots 0.17 em wide, 0.1 em apart, a 0.02 em margin before and -0.06 em after. */
    const val ColonEm = 0.13f
    const val ColonDotRadiusEm = 0.085f
    const val ColonDotOffsetEm = 0.135f
    const val ColonDotCenterXEm = 0.105f

    /** The line box of the digits, as a share of their size (line-height 0.8). */
    const val LineHeightEm = 0.8f

    /** The digits' size is this share of the dock's height (370 on 412). */
    const val HeightShare = 0.898f

    /** A small correction so the digits sit where the design has them inside its 0.8 em line box. */
    const val LiftEm = 0.012f

    /** Never wider than this share of the free width, so an unusual screen shape cannot clip the time. */
    const val WidthFill = 1f

    /** With a notification the clock shrinks to 316/370 of its size; the strip sits 22 dp under it. */
    const val StripScale = 0.854f
    val StripGap = 22.dp
    val StripTopPad = 6.dp
    val StripIcon = 18.dp
    val StripIconGap = 10.dp

    /**
     * Each digit leans a little, so the time looks set by hand. The guide gives 1 -3, 2 +5, 3 -4, 0 +5; the others
     * carry on in the same +-3 to +-5 range.
     */
    fun rotationDegrees(digit: Char): Float = when (digit) {
        '0' -> 5f
        '1' -> -3f
        '2' -> 5f
        '3' -> -4f
        '4' -> 4f
        '5' -> -5f
        '6' -> 3f
        '7' -> -4f
        '8' -> -3f
        '9' -> 5f
        else -> 0f
    }
}
