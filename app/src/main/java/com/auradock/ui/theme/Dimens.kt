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
    /** The frames draw the preview 157 high plus its 1 dp border on each side. */
    val thumbnailHeight: Dp = 159.dp
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

    /** The lowest opacity of the pulsing meeting line. */
    const val MeetingPulseLow = 0.45f

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

/**
 * The Bold template's clock (docs/design/STYLE_GUIDE.md). Numbers in "em" are a fraction of the font size and are measured on the
 * drawn glyphs (their ink), not on the font's advance widths.
 */
object AuraDockBold {
    /** Digits are drawn at 80% opacity, so overlapping ones screen into a lighter tone. */
    const val DigitAlpha = 0.8f

    /** Overall brightness of the clock (burn-in safe): the finished layer is drawn at this opacity. */
    const val Brightness = 0.85f

    /** The frosted feel: each glyph is a touch lighter at its top edge (white at this opacity, fading out over [FrostReach] of its height). */
    const val FrostAlpha = 0.02f
    const val FrostReach = 0.35f

    /** Each digit's ink overlaps the previous digit's ink by this much, for every pair. */
    const val OverlapEm = 0.06f

    /** The time's ink box is this share of the safe width, but never taller than [HeightShare] of the safe height. */
    const val WidthShare = 0.9f
    const val HeightShare = 0.8f

    /** Colon: two dots this wide (about a quarter of the digit height, as in the reference photos), centred at these shares of the digit height. */
    const val ColonDiameterEm = 0.165f
    const val ColonUpper = 0.33f
    const val ColonLower = 0.67f

    /** The colon's dots reach into the digit on each side by this share of a dot's diameter. */
    const val ColonOverlap = 0.3f

    /** Each digit leans a little, so the time looks set by hand (clockwise degrees): 1 -3, 2 +5, 3 -4, 0 +5; the others stay in the same 3 to 5 range. */
    fun tiltDegrees(digit: Char): Float = when (digit) {
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

    /** The safe area: the same 40 dp at the sides and 24 dp above and below that the other templates keep. */
    val SafeSide = AuraDockDockLayout.screenPadding
    val SafeVertical = AuraDockDockLayout.safeVertical

    /** The colour dots under the Bold card in the gallery: a 28 dp dot in a 44 dp touch target (the row starts that far in, so the dots line up with the text). */
    val PaletteDot = 28.dp
    val PaletteHit = 44.dp
    val PaletteDotRoom = (44 - 28).dp / 2

    /** The notification strip sits this far under the clock's area. */
    val StripGap = 22.dp
    val StripIcon = 18.dp
    val StripIconGap = 10.dp
    /** Space between the notification line and the held-for-you caption under it. */
    val StripLineGap = 6.dp
}

/** The Split dock (frames SplitDock-*): each half is 405 x 364, laid out in the frames' 915 x 412. */
object AuraDockSplit {
    val HalfWidth = 405.dp
    val HalfHeight = 364.dp
    val SideInset = 40.dp
    val TopInset = 24.dp
    val Gap = 25.dp

    /** The distance one swipe moves a widget: the frame's mid-swipe has the old one 78 up and the new one 118 down at 40%, so 196. */
    val Pitch = 196.dp
    const val OutgoingSlope = 1.8f
    const val IncomingSlope = 1.5f
    val BlurMax = 10.dp
    /** A release past this share of the pitch (or a quick flick) completes the swipe. */
    const val CommitShare = 0.3f
    const val FlickVelocity = 900f

    /** The dots that show where you are in a stack: 6 dp, 8 apart, 16 from the screen edge. */
    val IndicatorDot = 6.dp
    val IndicatorGap = 8.dp
    val IndicatorInset = 16.dp
    const val IndicatorHoldMillis = 1400

    /** With a notification the halves scale to 85% and rise 14 dp. */
    const val NotificationScale = 0.85f
    val NotificationRise = 14.dp

    /** The small corner time (when neither half shows the time) sits in the top margin. */
    val CornerTimeTop = 2.dp

    // The Dial (330 dp square): all in dp, from its centre.
    val DialSize = 330.dp
    const val DialRingRadius = 151.2f
    const val DialDotRadius = 1.8f
    const val DialTickInner = 140f
    const val DialTickOuter = 156f
    const val DialTickStroke = 4f
    const val DialNumeralRadius = 112f
    const val DialHourLength = 82f
    const val DialHourStroke = 11f
    const val DialMinuteLength = 128f
    const val DialMinuteStroke = 6f
    const val DialSecondLength = 140f
    const val DialSecondTail = 24f
    const val DialSecondStroke = 2.5f
    const val DialHubRadius = 7f
    const val DialHubHole = 2.5f

    // Month.
    val MonthRow = 44.dp
    val MonthTodayDisc = 38.dp
    val MonthEventDot = 4.dp
    val MonthSidePad = 6.dp
    val MonthTitleInset = 14.dp

    // Edit mode: the panel covers the half being edited; the other half drops to 25%.
    val EditPanelWidth = 425.dp
    val EditPanelHeight = 372.dp
    val EditPanelRadius = 28.dp
    const val EditOtherAlpha = 0.25f
    val EditRow = 56.dp
    val EditThumbWidth = 57.dp
    val EditThumbHeight = 51.dp
    const val EditThumbScale = 0.14f
    val EditBadge = 24.dp
    val EditButton = 44.dp

    // Add widget sheet.
    val SheetWidth = 776.dp
    val SheetHeight = 348.dp
    const val SheetScrim = 0.6f
    val SheetTileWidth = 81.dp
    val SheetTileHeight = 73.dp
    const val SheetTileScale = 0.2f
    const val DimAddOpacity = 0.35f
}

/** The World dock (frames Dock-World*): sizes in the design frame's units, drawn at the frame's scale (see WorldProjection). */
object AuraDockWorld {
    /** Land dots 3, the home dot 8, a city dot 4, all with a 2 stroke in the stage's black; the day and night line 2. */
    const val LandDotRadius = 3f
    const val HomeDotRadius = 8f
    const val CityDotRadius = 4f
    const val DotStroke = 2f
    const val LineStroke = 2f

    /** The line fades out toward the poles' ends: full strength down to this height in the frame, 10% at [LineFadeEnd], not drawn below [LineClip]. */
    const val LineFadeStart = 110f
    const val LineFadeEnd = 345f
    const val LineClip = 347f
    const val LineMinAlpha = 0.10f

    /** The map is recalculated this often: the day and night line barely moves in five minutes. */
    const val RefreshMillis = 5 * 60_000L

    /** A city's label pill: 6 radius, 7 x 3 padding, 5 between its two words, 9 to the right of its dot. */
    val PillRadius = 6.dp
    val PillPadH = 7.dp
    val PillPadV = 3.dp
    val PillGap = 5.dp
    const val PillDotOffset = 9f

    /** The home label sits 40 from the left and 24 from the bottom (70 with a notification), 18 between the city and the time. */
    val SideInset = 40.dp
    val BottomInset = 24.dp
    val BottomInsetNotification = 70.dp
    val LabelGap = 18.dp
    val LabelGapNotification = 15.dp

    /** The time's line box is 0.8 em high with its baseline 0.072 em above the bottom (Nunito's metrics in the frame's CSS, kept for Host Grotesk). */
    const val TimeLineEm = 0.8f
    const val TimeBaselineEm = 0.072f

    /** At night the home city is dimmed to 70% and the time goes grey. */
    const val NightCityAlpha = 0.7f

    /** The most extra cities the founder variant takes. */
    const val MaxExtraCities = 2
}

/**
 * The few offsets that make the dashboard match the handoff's empty-day frame (Dashboard-Empty), measured against it on the emulator:
 * Compose places the same text a little differently from the browser, so the header is nudged to bring the cards to the frame's positions.
 */
object AuraDockEmptyDay {
    /** The greeting sits this much higher than the standard top padding puts it. */
    val TopTrim = 7.dp

    /** More space between the greeting and the date line. */
    val DateGapExtra = 3.5.dp

    /** More space between the date line and the Today card. */
    val HeaderBottomExtra = 1.dp

    /** The empty Today card has this much less under "Enjoy the quiet." than the standard spacing leaves. */
    val TodayBottomTrim = 2.5.dp

    /** The frame's calendar card is this much shorter underneath its one line. */
    val CalendarBottom = 11.5.dp
}
