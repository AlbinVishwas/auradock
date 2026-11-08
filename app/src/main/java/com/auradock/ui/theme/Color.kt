package com.auradock.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Every colour AuraDock uses. Nothing outside this file writes a colour literal (see docs/DESIGN.md).
 *
 * Two layers: the raw palette ([AuraDockPalette]: black, the grey scale, white, coral and its tint), and the meaning each colour
 * is given in the app ([AuraDockColors]). Screens use the meaning, so a redesign can re-point a meaning at another palette entry.
 */
object AuraDockPalette {
    val Black = Color(0xFF000000)

    // The grey scale, darkest to lightest. The number is roughly how far it is from white.
    val Grey950 = Color(0xFF0D0D0D)
    val Grey900 = Color(0xFF141414)
    val Grey850 = Color(0xFF2A2A2A)
    val Grey800 = Color(0xFF3F3F3F)
    val Grey750 = Color(0xFF444444)
    val Grey700 = Color(0xFF5E5E5E)
    val Grey650 = Color(0xFF666666)
    val Grey600 = Color(0xFF6E6E6E)
    val Grey550 = Color(0xFF727272)
    val Grey500 = Color(0xFF8A8A8A)
    val Grey400 = Color(0xFFAAAAAA)
    val Grey200 = Color(0xFFD2D2D2)
    val White = Color(0xFFFFFFFF)

    // The style guide's scale (docs/design/STYLE_GUIDE.md), darkest to lightest.
    val Card = Color(0xFF161616)
    val Sheet = Color(0xFF1C1C1E)
    val Field = Color(0xFF262626)
    val QuietPill = Color(0xFF2E2E2E)
    val Inactive = Color(0xFF3A3A3A)
    val Outline = Color(0xFF5C5C5C)
    val Tertiary = Color(0xFF8A8A8A)
    val Secondary = Color(0xFFA3A3A3)

    /** Handoff v1: the tile behind an icon on a row, the hairline between rows, the border of a "Default" badge. */
    val Tile = Color(0xFF222222)
    val Hairline = Color(0xFF242424)
    val BadgeBorder = Color(0xFF3A3A33)
    /** The filled circle of a done task. */
    val DoneCircle = Color(0xFF333333)
    /** The outline of a navigation tile on the dashboard. */
    val TileOutline = Color(0xFF222222)
    /** The grab handle of a sheet, and the dimmed rows of a time wheel. */
    val Handle = Color(0xFF4A4A4A)
    val WheelDim = Color(0xFF6B6B6B)

    /** The app's accent: a pale cream. The one filled action, toggles, selected states, overdue items, badges. */
    val Cream = Color(0xFFFEFFED)

    /** Dock templates only (the Bold clock's two tones, and the brand mark): the aura in the logo. */
    val Coral = Color(0xFFFF5A4E)

    /** The lighter coral of the Bold clock. */
    val CoralLight = Color(0xFFFF8F86)

    /** A lighter, softer coral for text and fills that sit on black. */
    val Salmon = Color(0xFFFF9A90)

    // Translucent whites, for lines and glass on black.
    val White5 = Color(0x0AFFFFFF)
    val White8 = Color(0x14FFFFFF)
    val White9 = Color(0x17FFFFFF)
    val White12 = Color(0x1FFFFFFF)
    val White15 = Color(0x26FFFFFF)
    val White20 = Color(0x33FFFFFF)
    val White40 = Color(0x66FFFFFF)

    // Colours with a job of their own, kept from the first designs.
    val Blue = Color(0xFF2979FF)
    val Cyan = Color(0xFF4FC3F7)
    val Amber = Color(0xFFFFB300)
    val Green = Color(0xFF4CAF50)
    val DarkRed = Color(0xFF8E2A2A)
    val DimBlueGrey = Color(0xFF55616A)
    val OldGold = Color(0xFFB8892D)
    val AppBlue = Color(0xFF3A4A5A)
    val NightBlue = Color(0xFF151A20)
}

object AuraDockColors {
    // Surfaces
    /** Pure ambient black: the stage everything sits on. */
    val Background = AuraDockPalette.Black
    /** Cards. */
    val Surface = AuraDockPalette.Card
    /** Raised surface: bottom sheets. */
    val SurfaceRaised = AuraDockPalette.Sheet
    /** Chips, text fields and other controls that sit on a card. */
    val Field = AuraDockPalette.Field
    /** The quiet pill: secondary buttons. */
    val QuietPill = AuraDockPalette.QuietPill
    /** Off states: a switched-off toggle track, an empty checkbox, an unavailable control. */
    val Inactive = AuraDockPalette.Inactive
    /** Outlines of empty checkboxes and rings. */
    val Outline = AuraDockPalette.Outline
    /** Text of done items. */
    val Tertiary = AuraDockPalette.Tertiary
    /** The rounded tile behind an icon on a row (permission rows, nav tiles). */
    val IconTile = AuraDockPalette.Tile
    /** The thin line between rows inside a card. */
    val RowDivider = AuraDockPalette.Hairline
    /** The outline of a "Default" badge. */
    val BadgeBorder = AuraDockPalette.BadgeBorder
    /** The current progress dot: white (the handoff), not the accent. */
    val DotCurrent = AuraDockPalette.White
    val DoneCircle = AuraDockPalette.DoneCircle
    /** The band that marks the chosen row of a time wheel. */
    val WheelHighlight = AuraDockPalette.DoneCircle
    val SheetHandle = AuraDockPalette.Handle
    val WheelDim = AuraDockPalette.WheelDim
    val TileOutline = AuraDockPalette.TileOutline
    val GlassCardTop = AuraDockPalette.White9
    val GlassCardBottom = AuraDockPalette.White5
    val GlassBorder = AuraDockPalette.White12
    val Divider = AuraDockPalette.White15

    // Text
    val OnBackground = AuraDockPalette.White
    val OnSurfaceVariant = AuraDockPalette.Secondary
    /** Text that is switched off (an unavailable row). */
    val OnDisabled = AuraDockPalette.Outline

    // The app's accent (cream) and the text that sits on it.
    val Accent = AuraDockPalette.Cream
    val OnAccent = AuraDockPalette.Black

    /** The scrim behind a sheet: black at 60%. */
    val Scrim = AuraDockPalette.Black.copy(alpha = AuraDockMotion.ScrimAlpha)
    val Overdue = Accent

    // Dock templates only. Never in the app's own UI.
    val Coral = AuraDockPalette.Coral
    val CoralLight = AuraDockPalette.CoralLight
    val CoralTint = AuraDockPalette.Salmon

    /** The two big dots of the Bold clock's colon: translucent grey. */
    val BoldColon = Color(0xFFC8C8C8).copy(alpha = 0.8f)

    // The dock templates: pure black, white and the greys, with coral the one loud colour.
    val DockText = AuraDockPalette.White
    val DockSecondary = AuraDockPalette.Secondary
    val DockLabel = AuraDockPalette.Tertiary
    val DockOverdue = AuraDockPalette.Coral
    /** The line that shows how long this focus session has gone on: quiet greys, never coral. */
    val SessionFill = AuraDockPalette.Tertiary
    val SessionTrack = AuraDockPalette.Outline.copy(alpha = 0.4f)

    /** The same line on Minimal, which is dimmed for OLED: one step darker. */
    val SessionFillDim = AuraDockPalette.Outline
    val SessionTrackDim = AuraDockPalette.Inactive.copy(alpha = 0.6f)
    val DockDivider = AuraDockPalette.Inactive
    /** Minimal: OLED-dim grey digits. */
    val DimDigits = AuraDockPalette.Outline
    val DimColon = AuraDockPalette.Inactive
    val DimLabel = AuraDockPalette.Inactive
    /** The notification strip: grey, quieter than anything else. */
    val StripText = AuraDockPalette.Tertiary
    val StripTextDim = AuraDockPalette.Inactive

    // The docking pill and the onboarding animation
    val PillBackground = AuraDockPalette.Black
    val PillText = AuraDockPalette.White
    val PillBorder = AuraDockPalette.White20
    val IllustrationStand = AuraDockPalette.Grey750
    val IllustrationPhone = AuraDockPalette.Grey850
    val IllustrationApp = AuraDockPalette.AppBlue
    val IllustrationAppLine = AuraDockPalette.White40
    val IllustrationDimScreen = AuraDockPalette.NightBlue
    val IllustrationRingTrack = AuraDockPalette.White20
}
