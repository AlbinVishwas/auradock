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
    /** The tick box of the app picker. */
    val checkbox: Dp = 7.dp
    /** The outlined "Default" / "In use" badge. */
    val badge: Dp = 10.dp
    /** Handoff v1: rows inside a screen (permission rows, task-free list cards, nav tiles) use 20 dp, not the style guide's 24. */
    val row: Dp = 20.dp
    /** The 40 dp tile behind an icon on a row. */
    val iconTile: Dp = 12.dp
    /** The letter tile on an app chip: 9 dp corners. */
    val chipTile: Dp = 9.dp
    /** The text field and the time row in a sheet: 16 dp corners. */
    val field: Dp = 16.dp
    /** The dock thumbnail on the dashboard, and the 44 dp calendar tile. */
    val thumbnail: Dp = 16.dp
    val calendarTile: Dp = 14.dp
    /** A template card in the carousel. */
    val templateCard: Dp = 22.dp
    /** The device frame on the welcome step. */
    val device: Dp = 26.dp
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
    /** Dashboard (handoff): the dock thumbnail is 157 dp high; a task row's tap area is 38 dp wide (circle + gap); the 44 dp calendar tile. */
    val thumbnailHeight: Dp = 157.dp
    val taskCircleTap: Dp = 38.dp
    val calendarTileSize: Dp = 44.dp
    val calendarAllow: Dp = 40.dp
    val timeColumn: Dp = 64.dp
    val navTileIcon: Dp = 22.dp
    val pillIcon: Dp = 22.dp
    /** Sheets (handoff): the grab handle is 32 x 4; a field is 56 high; the time wheel is 168 high with 48 dp rows. */
    val handleWidth: Dp = 32.dp
    val handleHeight: Dp = 4.dp
    val field: Dp = 56.dp
    val wheelHeight: Dp = 168.dp
    val wheelRow: Dp = 48.dp
    val wheelHighlightInset: Dp = 12.dp
    val rowChevron: Dp = 18.dp
    /** Focus (handoff): an app chip is 40 high with a 28 dp tile; the search bar is 48; an app row is 60 with a 40 dp tile. */
    val appChip: Dp = 40.dp
    val searchBar: Dp = 48.dp
    val appRow: Dp = 60.dp
    /** The seven-day mini bar on the dashboard: 6 dp wide bars 4 dp apart, 4 to 20 dp tall, a 3 dp dot for an empty day. */
    val weekBar: Dp = 6.dp
    val weekBarGap: Dp = 4.dp
    val weekBarMin: Dp = 4.dp
    val weekBarMax: Dp = 20.dp
    val weekBarEmpty: Dp = 3.dp
    /** The small pills on a row: Allow, Skip. 36 dp high. */
    val smallPill: Dp = 36.dp
    /** The dots under a carousel: 6 dp. */
    val pagerDot: Dp = 6.dp
    /** The check inside an "In use" badge. */
    val badgeIcon: Dp = 11.dp
    /** The round "allowed" check on a row, and the check inside it. */
    val allowedCheck: Dp = 28.dp
    val allowedCheckIcon: Dp = 14.dp
    /** The 20 dp icon inside an icon tile, and the 22 dp icon on a nav tile or fact row. */
    val tileIcon: Dp = 20.dp
    /** The check beside "Allowed" on the Setup page. */
    val statusIcon: Dp = 12.dp
    val factIcon: Dp = 22.dp
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
    /** The height of the weekday and month above Classic's day number (two 34 sp lines). */
    val classicHeading: Dp = 94.dp
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

    /** Space between the last notification line and the held-for-you counts beside it. */
    val captionGap: Dp = 16.dp

    /** The focus session line: 160 x 2 dp, 12 dp above the bottom edge (inside the burn-in drift), one lap an hour. */
    val sessionLineWidth: Dp = 160.dp
    val sessionLineHeight: Dp = 2.dp
    val sessionLineBottom: Dp = 12.dp
    const val SessionLapMinutes = 60

    /** The quiet lines under the clock: one row each, the circle 16 dp, at most 560 dp wide. */
    val lineRow: Dp = 36.dp
    val lineCircle: Dp = 16.dp
    val lineGap: Dp = 12.dp
    val lineMaxWidth: Dp = 560.dp

    /** The morning prompt: a column 600 dp wide. */
    val promptWidth: Dp = 600.dp
    val promptGap: Dp = 20.dp
}

/** The onboarding animation's drawing sizes. */
object AuraDockIllustration {
    /** The screensaver step's scene is drawn on a 320 x 210 canvas; its clock is coral at 90%. */
    val sceneWidth: Dp = 320.dp
    val sceneHeight: Dp = 210.dp
    const val ClockAlpha = 0.9f
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
    /** Handoff v1: the carousel card is 320 x 144, the next one peeks in after a 16 dp gap; neighbours sit at 88% and 40% opacity. */
    val cardWidth: Dp = 320.dp
    val cardHeight: Dp = 144.dp
    val cardGap: Dp = 16.dp
    val textInset: Dp = 46.dp
    val welcomeSubtitleWidth: Dp = 300.dp
    const val NeighbourScale = 0.88f
    const val NeighbourAlpha = 0.4f
    /** A row of the app picker that cannot be chosen (the limit is reached) fades to 35%. */
    const val DimmedAlpha = 0.35f
    val deviceWidth: Dp = 340.dp
    val deviceHeight: Dp = 160.dp
    val deviceBorder: Dp = 6.dp
    val deviceNeck: Dp = 14.dp
    val deviceNeckWidth: Dp = 120.dp
    val deviceBaseWidth: Dp = 200.dp
    val deviceBaseHeight: Dp = 8.dp
    val pageInset: Dp = 40.dp
    val previewDesignWidth: Dp = 914.dp
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

    /** A small nudge to the left, measured against the handoff render: the digits' ink sits slightly right of the design's. */
    const val ShiftEm = 0f

    /** Extra room inside the blend layer, either side of the time, so a leaning digit is never clipped. */
    const val BleedEm = 0.12f

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
    /** Space between the notification line and the held-for-you caption under it. */
    val StripLineGap = 6.dp

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
