package com.auradock.ui.theme

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
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

/** Display face: the clock, big numbers and screen titles. Rounded, friendly. */
val NunitoFamily = FontFamily(Weights.map { weightOf(R.font.nunito_variable, it) })

/** Titles, section headings, buttons and numbers in the app (not on the dock). */
val MonaSansFamily = FontFamily(Weights.map { weightOf(R.font.mona_sans_variable, it) })

/** UI face: body text and secondary text. */
val InterFamily = FontFamily(Weights.map { weightOf(R.font.inter_variable, it) })

/**
 * The type scale. Screens pick a style from here and never set a font, size or weight themselves (see docs/DESIGN.md).
 * Display styles use Nunito; UI styles use Inter.
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

    // Names the screens used before the style guide. They now map onto the guide's scale.
    val Title = Button
    val TitleStrong = Button
    val WidgetPrimary = Section
    val PillText = Body
    val PillAction = Button
    val WidgetLabel = Caption
    val CalendarCell = Caption

    /** 96 sp, extra light: the clock on the dashboard's preview (the dock has its own, see below). */
    val ClockDisplay = TextStyle(fontFamily = NunitoFamily, fontWeight = FontWeight.ExtraLight, fontSize = 96.sp, letterSpacing = (-4).sp)

    // ---- The dock (landscape, read from a distance; Nunito, light) ----
    /** 13 sp, light, wide: the date under the clock. */
    val ClockCaption = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Light, fontSize = 13.sp, letterSpacing = 2.sp)

    /** 18 sp: the AM/PM marker beside the clock. */
    val ClockMeridiem = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Light, fontSize = 18.sp)

    /** 12 sp, medium, wide: a section label on the dock ("NEXT", "TASK"). */
    val DockLabel = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 2.sp)

    /** 14 sp: a line in the notification strip. */
    val DockStrip = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp)

    /** 16 sp, light: the weather condition line. */
    val DockCondition = TextStyle(fontFamily = NunitoFamily, fontWeight = FontWeight.Light, fontSize = 16.sp)

    /** 20 sp, light: a time or secondary line. */
    val DockSecondary = TextStyle(fontFamily = NunitoFamily, fontWeight = FontWeight.Light, fontSize = 20.sp)

    /** 24 sp, light: an event title. */
    val DockBody = TextStyle(fontFamily = NunitoFamily, fontWeight = FontWeight.Light, fontSize = 24.sp)

    /** 26 sp, light: a task title. */
    val DockTask = TextStyle(fontFamily = NunitoFamily, fontWeight = FontWeight.Light, fontSize = 26.sp)

    /** 36 sp, light: temperature on the Focus template. */
    val DockTemperature = TextStyle(fontFamily = NunitoFamily, fontWeight = FontWeight.Light, fontSize = 36.sp)

    /** 40 sp, light: temperature on the Classic template. */
    val DockTemperatureLarge = TextStyle(fontFamily = NunitoFamily, fontWeight = FontWeight.Light, fontSize = 40.sp)

    /** 24 sp, light: the clock in the onboarding illustration. */
    val IllustrationClock = TextStyle(fontFamily = NunitoFamily, fontWeight = FontWeight.Light, fontSize = 28.sp)
}
