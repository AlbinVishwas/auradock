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
    FontWeight.ExtraLight, FontWeight.Light, FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold
)

/** Display face: the clock, big numbers and screen titles. Rounded, friendly. */
val NunitoFamily = FontFamily(Weights.map { weightOf(R.font.nunito_variable, it) })

/** UI face: labels, body text, buttons. */
val InterFamily = FontFamily(Weights.map { weightOf(R.font.inter_variable, it) })

/**
 * The type scale. Screens pick a style from here and never set a font, size or weight themselves (see docs/DESIGN.md).
 * Display styles use Nunito; UI styles use Inter.
 */
object AuraDockTypography {
    // ---- Display (Nunito) ----
    /** 96 sp, extra light: the main clock digits. */
    val ClockDisplay = TextStyle(fontFamily = NunitoFamily, fontWeight = FontWeight.ExtraLight, fontSize = 96.sp, letterSpacing = (-4).sp)

    /** 32 sp, semibold: the title at the top of a screen. */
    val ScreenTitle = TextStyle(fontFamily = NunitoFamily, fontWeight = FontWeight.SemiBold, fontSize = 32.sp, lineHeight = 38.sp)

    /** 28 sp, light: a primary widget value (temperature, a dash for "no data"). */
    val WidgetPrimary = TextStyle(fontFamily = NunitoFamily, fontWeight = FontWeight.Light, fontSize = 28.sp, letterSpacing = (-0.5).sp)

    // ---- UI (Inter) ----
    /** 18 sp, medium: a row or card title. */
    val Title = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Medium, fontSize = 18.sp)

    /** 18 sp, semibold: the same, emphasised (the item in use). */
    val TitleStrong = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)

    /** 16 sp, regular: running text. */
    val Body = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp)

    /** 16 sp, medium: the text on the docking pill. */
    val PillText = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Medium, fontSize = 16.sp)

    /** 16 sp, semibold: the action on the docking pill. */
    val PillAction = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)

    /** 14 sp, medium: widget labels, city name, buttons. */
    val WidgetLabel = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, letterSpacing = 0.5.sp)

    /** 12 sp, regular: calendar day cells. */
    val CalendarCell = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp, letterSpacing = 0.sp)

    /** 10 sp, medium: captions and explanations. */
    val Caption = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Medium, fontSize = 10.sp, letterSpacing = 1.sp)

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
