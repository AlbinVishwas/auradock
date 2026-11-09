package com.auradock.ui.theme

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.auradock.R

/** One weight of a variable font file (both fonts ship as a single file with a weight axis). */
@OptIn(ExperimentalTextApi::class)
private fun weightOf(resId: Int, weight: FontWeight) = Font(
    resId = resId,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight))
)

private val Weights = listOf(
    FontWeight.ExtraLight, FontWeight.Light, FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold,
    FontWeight.ExtraBold, FontWeight.Black
)

/** Nunito: used only for the Bold clock's digits (and its illustration). Everything else big uses [DisplayGrotesk]. */
val NunitoFamily = FontFamily(Weights.map { weightOf(R.font.nunito_variable, it) })

/** Display face for big numbers off the Bold clock: temperatures, the Classic day number, the Minimal and Productivity clocks (Host Grotesk, SIL OFL). Its heaviest weight is ExtraBold. */
val DisplayGrotesk = FontFamily(Weights.map { weightOf(R.font.host_grotesk_variable, it) })

/** The Focus template's giant, tall digits: a condensed display face (Oswald, SIL OFL). */
val CondensedFamily = FontFamily(Weights.map { weightOf(R.font.oswald_variable, it) })

/** Titles, section headings, buttons and numbers in the app (not on the dock). */
val MonaSansFamily = FontFamily(Weights.map { weightOf(R.font.mona_sans_variable, it) })

/** UI face: body text and secondary text. */
val InterFamily = FontFamily(Weights.map { weightOf(R.font.inter_variable, it) })

/**
 * The type scale. Screens pick a style from here and never set a font, size or weight themselves (see docs/DESIGN.md).
 * Display styles use Host Grotesk (Nunito only on the Bold clock); UI styles use Inter.
 */
object AuraDockTypography {
    // ---- The app (style guide): Mona Sans for titles, headings, buttons and numbers; Inter for reading ----
    /** Title: Mona Sans ExtraBold 34 / 1.1. The one big line at the top of a screen. */
    val ScreenTitle = TextStyle(fontFamily = MonaSansFamily, fontWeight = FontWeight.ExtraBold, fontSize = 34.sp, lineHeight = 37.4.sp)

    /** Section: Mona Sans Bold 22. "Today", a card heading. */
    val Section = TextStyle(fontFamily = MonaSansFamily, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 26.sp)

    /** Button and numbers: Mona Sans Bold 18. The text on a pill, a temperature, a time. */
    val Button = TextStyle(fontFamily = MonaSansFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 22.sp)

    /** Body: Inter Medium 16 / 1.4. A task, a row, running text. */
    val Body = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 22.4.sp)

    /** Secondary: Inter Regular 15. Colour it with [AuraDockColors.OnSurfaceVariant]. */
    val Secondary = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 21.sp)

    /** Caption: Inter Medium 13. "Overdue · 11:00 AM". */
    val Caption = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp)

    // ---- Handoff v1 (docs/design/handoff/claude-design-v1) ----
    /** The welcome title: Mona Sans ExtraBold 48, tight. */
    val WelcomeTitle = TextStyle(fontFamily = MonaSansFamily, fontWeight = FontWeight.ExtraBold, fontSize = 48.sp, lineHeight = 48.sp, letterSpacing = (-0.96).sp)

    /** The line under the welcome title: Inter Regular 20 / 1.35. */
    val WelcomeSubtitle = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Normal, fontSize = 20.sp, lineHeight = 27.sp)

    /** A step or screen title one size down from [ScreenTitle]: Mona Sans ExtraBold 32. */
    val StepTitle = TextStyle(fontFamily = MonaSansFamily, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp, lineHeight = 35.2.sp)

    /** The dashboard greeting: Mona Sans ExtraBold 40. */
    /** The greeting on the handoff's empty-day frame: 36 / 1.1, tracking -0.01 em. */
    val GreetingCompact = TextStyle(fontFamily = MonaSansFamily, fontWeight = FontWeight.ExtraBold, fontSize = 36.sp, lineHeight = 39.6.sp, letterSpacing = (-0.36).sp)
    val Greeting = TextStyle(fontFamily = MonaSansFamily, fontWeight = FontWeight.ExtraBold, fontSize = 40.sp, lineHeight = 44.sp, letterSpacing = (-0.4).sp)

    /** Running text under a step title: Inter Regular 17 / 1.45. */
    val StepBody = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Normal, fontSize = 17.sp, lineHeight = 24.65.sp)

    /** A row's title: Inter SemiBold 15. */
    val RowTitle = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp)

    /** A row's reason: Inter Regular 13 / 1.35. */
    val RowCaption = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 17.55.sp)

    /** The text on a small pill (Allow): Inter SemiBold 13. */
    val SmallAction = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)

    /** The text of a badge: Inter SemiBold 12. */
    val SmallBadge = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)

    /** A sheet's title: Mona Sans Bold 24. */
    val SheetTitle = TextStyle(fontFamily = MonaSansFamily, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 28.sp)

    /** A field's label (Inter Medium 13) and its text (Inter Medium 17). */
    val FieldLabel = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp)
    val FieldText = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Medium, fontSize = 17.sp, lineHeight = 24.sp)

    /** The time wheel: the chosen number in Mona Sans Bold 28 (24 for AM/PM), the others 20. */
    val WheelChosen = TextStyle(fontFamily = MonaSansFamily, fontWeight = FontWeight.Bold, fontSize = 28.sp)
    val WheelChosenSmall = TextStyle(fontFamily = MonaSansFamily, fontWeight = FontWeight.Bold, fontSize = 24.sp)
    val WheelOther = TextStyle(fontFamily = MonaSansFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp)

    /** A number on the undock card: Mona Sans Bold 28. */
    val SummaryValue = TextStyle(fontFamily = MonaSansFamily, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 32.sp)

    /** A settings card's title: Inter SemiBold 17. */
    val CardTitle = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 24.sp)

    /** A line in a row ("Choose apps", "Setup and permissions"): Inter Medium 15. */
    val RowLabel = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 20.sp)

    /** An app chip's name: Inter Medium 14. */
    val ChipLabel = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp)

    /** The letter on an app tile: Mona Sans ExtraBold 14, or 17 on the picker's bigger tile. */
    val TileLetter = TextStyle(fontFamily = MonaSansFamily, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
    val TileLetterLarge = TextStyle(fontFamily = MonaSansFamily, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)

    /** A template's name under the carousel: Mona Sans Bold 24. */
    val TemplateName = TextStyle(fontFamily = MonaSansFamily, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 28.sp)

    /** "Nothing planned. Enjoy the quiet.": Mona Sans Bold 20 / 1.3. */
    val EmptyState = TextStyle(fontFamily = MonaSansFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp)

    /** The count beside a card's title ("3 left") and a task's time: Inter Regular 14. */
    val Meta = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp)

    /** The time of an event: Mona Sans Bold 18, with its "PM" in [EventMeridiem]. */
    val EventTime = TextStyle(fontFamily = MonaSansFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 21.6.sp)
    val EventMeridiem = TextStyle(fontFamily = MonaSansFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp)

    /** The text on the calendar's Allow pill: Inter SemiBold 14. */
    val PillSmall = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)

    // Names the screens used before the style guide. They now map onto the guide's scale.
    val Title = Button
    val TitleStrong = Button
    val WidgetPrimary = Section
    val PillText = Body
    val PillAction = Button
    val WidgetLabel = Caption
    val CalendarCell = Caption

    /** 96 sp, extra light: the clock on the dashboard's preview (the dock has its own, see below). */
    val ClockDisplay = TextStyle(fontFamily = DisplayGrotesk, fontWeight = FontWeight.ExtraLight, fontSize = 96.sp, letterSpacing = (-4).sp)

    // ---- The dock (landscape, read from a distance). Host Grotesk for the big shapes, Inter for the reading. ----
    /** Focus: giant condensed digits, Oswald Bold. The size is worked out from the screen; this sets the face. */
    val DockCondensed = TextStyle(
        fontFamily = CondensedFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 200.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both)
    )

    /** The Classic template's weekday, coral: Host Grotesk ExtraBold 34. */
    val DockWeekday = TextStyle(fontFamily = DisplayGrotesk, fontWeight = FontWeight.ExtraBold, fontSize = 34.sp)

    /** The Classic template's month, grey: Host Grotesk Bold 34. */
    val DockMonth = TextStyle(fontFamily = DisplayGrotesk, fontWeight = FontWeight.Bold, fontSize = 34.sp)

    /** The Classic template's huge day number: Host Grotesk ExtraBold 250. */
    val DockDayNumber = TextStyle(fontFamily = DisplayGrotesk, fontWeight = FontWeight.ExtraBold, fontSize = 250.sp, letterSpacing = (-6).sp)

    /** A big temperature: Host Grotesk ExtraBold 88. */
    val DockTemperatureLarge = TextStyle(fontFamily = DisplayGrotesk, fontWeight = FontWeight.ExtraBold, fontSize = 88.sp, letterSpacing = (-2).sp)

    /** A medium temperature (Focus, Productivity): Host Grotesk ExtraBold 44. */
    val DockTemperature = TextStyle(fontFamily = DisplayGrotesk, fontWeight = FontWeight.ExtraBold, fontSize = 44.sp)

    /** The city and other short headings on the dock: Mona Sans Bold 20. */
    val DockHeading = TextStyle(fontFamily = MonaSansFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp)

    /** Secondary lines on the dock (condition, a time, a date): Inter Regular 18. */
    val DockSecondary = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Normal, fontSize = 18.sp)

    /** Condition under a temperature: Inter Medium 18. */
    val DockCondition = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Medium, fontSize = 18.sp)

    /** An event title on the dock: Inter Medium 22. */
    val DockBody = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Medium, fontSize = 22.sp)

    /** A task title on the dock: Inter Medium 24. */
    val DockTask = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Medium, fontSize = 24.sp)

    /** A section label on the dock ("Next", "Today"): Inter Medium 14, sentence case. */
    val DockLabel = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp)

    /** The quiet lines under the clock (intention, next meeting, pick up): Inter Medium 18. */
    val DockLine = DockCondition

    /** The morning question on the dock: Mona Sans Bold 30. */
    val DockPrompt = TextStyle(fontFamily = MonaSansFamily, fontWeight = FontWeight.Bold, fontSize = 30.sp)

    /** The notification strip: a quiet Inter Medium 13 caption. */
    val DockStrip = Caption

    /** The notification line under the Bold clock: Inter Medium 16. */
    val BoldStrip = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Medium, fontSize = 16.sp)

    // ---- The World dock (frames Dock-World*): the home city in Mona Sans Bold, the time in the display face (Host Grotesk where the frame says Nunito) ----
    /** The home city: Mona Sans Bold 30, coral, tight tracking. */
    val DockWorldCity = TextStyle(fontFamily = MonaSansFamily, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 33.sp, letterSpacing = (-0.9).sp)

    /** The same with a notification showing: 26. */
    val DockWorldCityCompact = TextStyle(fontFamily = MonaSansFamily, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 28.6.sp, letterSpacing = (-0.78).sp)

    /** The home time: Host Grotesk ExtraBold 120, line 0.8, tabular figures. */
    val DockWorldTime = TextStyle(
        fontFamily = DisplayGrotesk, fontWeight = FontWeight.ExtraBold, fontSize = 120.sp, lineHeight = 96.sp, letterSpacing = (-1.2).sp,
        fontFeatureSettings = "tnum", platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both)
    )

    /** The same with a notification showing: 102. */
    val DockWorldTimeCompact = DockWorldTime.copy(fontSize = 102.sp, lineHeight = 81.6.sp, letterSpacing = (-1.02).sp)

    /** A city's name on the map: Inter Medium 13 (grey); its time beside it is [DockWorldPillTime]. */
    val DockWorldPillLabel = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 16.sp)

    /** A city's time on the map: Mona Sans Bold 13, tabular figures. */
    val DockWorldPillTime = TextStyle(fontFamily = MonaSansFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, lineHeight = 16.sp, fontFeatureSettings = "tnum")

    // ---- The Split dock (frames SplitDock-*): sizes in the frames' px, which are dp here ----
    val SplitNumeral = TextStyle(fontFamily = MonaSansFamily, fontWeight = FontWeight.Bold, fontSize = 26.sp)
    val SplitMonthTitle = TextStyle(fontFamily = MonaSansFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp, letterSpacing = 1.6.sp)
    val SplitWeekday = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    /** The frames ask for Mona Sans SemiBold 18 here; the bundled Mona Sans file draws weights under Bold at its lightest, so Bold is used. */
    val SplitDay = TextStyle(fontFamily = MonaSansFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    val SplitToday = TextStyle(fontFamily = MonaSansFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    val SplitCornerTime = TextStyle(fontFamily = MonaSansFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp)

    /** The Digital widget: Host Grotesk ExtraBold 150, line 0.8, and its date line. */
    val SplitDigital = TextStyle(
        fontFamily = DisplayGrotesk, fontWeight = FontWeight.ExtraBold, fontSize = 150.sp, lineHeight = 120.sp, letterSpacing = (-1.5).sp,
        fontFeatureSettings = "tnum", platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both)
    )
    val SplitDigitalDate = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Medium, fontSize = 20.sp)

    /** Big date: "Fri Nov" in Mona Sans Bold 40, the day in Mona Sans Light 230 (line 0.8, tight). */
    val SplitBigDateHead = TextStyle(fontFamily = MonaSansFamily, fontWeight = FontWeight.Bold, fontSize = 40.sp, lineHeight = 40.sp)
    val SplitBigDateNumber = TextStyle(
        fontFamily = MonaSansFamily, fontWeight = FontWeight.Light, fontSize = 230.sp, lineHeight = 184.sp, letterSpacing = (-6.9).sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both)
    )

    /** Up next. */
    val SplitUpNextLabel = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Medium, fontSize = 15.sp)
    val SplitUpNextTime = TextStyle(fontFamily = MonaSansFamily, fontWeight = FontWeight.Bold, fontSize = 30.sp)
    val SplitUpNextTitle = TextStyle(fontFamily = MonaSansFamily, fontWeight = FontWeight.Bold, fontSize = 38.sp, lineHeight = 41.8.sp)
    val SplitUpNextSub = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Medium, fontSize = 17.sp)
    val SplitUpNextThen = TextStyle(fontFamily = MonaSansFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    val SplitUpNextThenTitle = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Medium, fontSize = 16.sp)

    /** Weather: the temperature is Host Grotesk ExtraBold 160 (the frame says Nunito). */
    val SplitWeatherCity = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Medium, fontSize = 17.sp)
    val SplitWeatherTemp = TextStyle(
        fontFamily = DisplayGrotesk, fontWeight = FontWeight.ExtraBold, fontSize = 160.sp, lineHeight = 128.sp, letterSpacing = (-3.2).sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both)
    )
    val SplitWeatherCondition = TextStyle(fontFamily = MonaSansFamily, fontWeight = FontWeight.Bold, fontSize = 24.sp)
    val SplitWeatherRange = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Medium, fontSize = 16.sp)

    /** Now playing. */
    val SplitTrack = TextStyle(fontFamily = MonaSansFamily, fontWeight = FontWeight.ExtraBold, fontSize = 31.sp, lineHeight = 34.1.sp)
    val SplitArtist = TextStyle(fontFamily = MonaSansFamily, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 26.4.sp)

    /** Tasks. */
    val SplitTasksTitle = TextStyle(fontFamily = MonaSansFamily, fontWeight = FontWeight.Bold, fontSize = 26.sp)
    val SplitTasksCount = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Medium, fontSize = 15.sp)
    val SplitTask = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Medium, fontSize = 20.sp)
    val SplitTaskSub = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp)
    val SplitTaskTime = TextStyle(fontFamily = MonaSansFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp)

    /** Focus ring: the time is Host Grotesk ExtraBold 68. */
    val SplitFocusLabel = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Medium, fontSize = 15.sp)
    val SplitFocusTime = TextStyle(
        fontFamily = DisplayGrotesk, fontWeight = FontWeight.ExtraBold, fontSize = 68.sp, lineHeight = 61.2.sp, fontFeatureSettings = "tnum",
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both)
    )

    /** The World widget's home label and time (half-size version of the World dock). */
    val SplitWorldCity = TextStyle(fontFamily = MonaSansFamily, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 26.4.sp, letterSpacing = (-0.72).sp)
    val SplitWorldTime = TextStyle(
        fontFamily = DisplayGrotesk, fontWeight = FontWeight.ExtraBold, fontSize = 92.sp, lineHeight = 73.6.sp, fontFeatureSettings = "tnum",
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both)
    )

    /** Edit mode and the Add widget sheet. */
    val SplitEditTitle = TextStyle(fontFamily = MonaSansFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    val SplitEditDone = TextStyle(fontFamily = MonaSansFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    val SplitEditRow = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Medium, fontSize = 15.sp)
    val SplitEditRowSub = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Normal, fontSize = 13.sp)
    val SplitEditAdd = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    val SplitSheetTitle = TextStyle(fontFamily = MonaSansFamily, fontWeight = FontWeight.Bold, fontSize = 22.sp)
    val SplitSheetGroup = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    val SplitSheetTile = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp)
    val SplitBadge = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)

    /** The AM/PM marker beside a clock: Inter Medium 18. */
    val ClockMeridiem = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Medium, fontSize = 18.sp)

    /** The date under a clock: Inter Medium 14. */
    val ClockCaption = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp)

    /** The clock drawn in the screensaver illustration: Nunito Black (the size is set where it is drawn). */
    val IllustrationBoldClock = TextStyle(fontFamily = NunitoFamily, fontWeight = FontWeight.Black)

    /** 28 sp, light: the clock in the onboarding illustration. */
    val IllustrationClock = TextStyle(fontFamily = DisplayGrotesk, fontWeight = FontWeight.Light, fontSize = 28.sp)
}
