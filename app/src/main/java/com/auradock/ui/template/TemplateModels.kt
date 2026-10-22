package com.auradock.ui.template

import androidx.compose.ui.graphics.Color
import com.auradock.ui.theme.AuraDockColors

data class StandbyTemplate(
    val id: String,
    val name: String,
    val layoutType: LayoutType,
    val widgets: List<WidgetConfig>,
    val style: StyleConfig
)

enum class LayoutType {
    MINIMAL,
    SPLIT,
    FOCUS,
    GRID
}

data class WidgetConfig(
    val type: WidgetType,
    val position: WidgetPosition
)

enum class WidgetType {
    CLOCK,
    WEATHER,
    CALENDAR,
    REMINDERS
}

enum class WidgetPosition {
    FULL,
    LEFT,
    RIGHT,
    TOP,
    BOTTOM
}

data class StyleConfig(
    val accentColor: Color,
    val theme: String
)

object TemplateDefaults {
    val Minimal = StandbyTemplate(
        id = "minimal",
        name = "Minimal",
        layoutType = LayoutType.MINIMAL,
        widgets = listOf(
            WidgetConfig(type = WidgetType.CLOCK, position = WidgetPosition.FULL)
        ),
        style = StyleConfig(accentColor = AuraDockColors.ClockGlow, theme = "dark")
    )

    val Split = StandbyTemplate(
        id = "split_weather",
        name = "Split Weather",
        layoutType = LayoutType.SPLIT,
        widgets = listOf(
            WidgetConfig(type = WidgetType.CLOCK, position = WidgetPosition.LEFT),
            WidgetConfig(type = WidgetType.WEATHER, position = WidgetPosition.RIGHT)
        ),
        style = StyleConfig(accentColor = AuraDockColors.AccentBlue, theme = "dark")
    )

    val FocusCalendar = StandbyTemplate(
        id = "focus_calendar",
        name = "Focus Calendar",
        layoutType = LayoutType.FOCUS,
        widgets = listOf(
            WidgetConfig(type = WidgetType.CALENDAR, position = WidgetPosition.FULL)
        ),
        style = StyleConfig(accentColor = AuraDockColors.AccentAmber, theme = "dark")
    )

    val GridMix = StandbyTemplate(
        id = "grid_mix",
        name = "Grid Mix",
        layoutType = LayoutType.GRID,
        widgets = listOf(
            WidgetConfig(type = WidgetType.CLOCK, position = WidgetPosition.TOP),
            WidgetConfig(type = WidgetType.REMINDERS, position = WidgetPosition.BOTTOM)
        ),
        style = StyleConfig(accentColor = Color(0xFF80CBC4), theme = "dark")
    )

    val gallery: List<StandbyTemplate> = listOf(Minimal, Split, FocusCalendar, GridMix)
}

fun StandbyTemplate.sanitizedForStandby(): StandbyTemplate {
    val clockWidget = widgets.firstOrNull { it.type == WidgetType.CLOCK }
        ?: WidgetConfig(type = WidgetType.CLOCK, position = WidgetPosition.TOP)

    val nonClockWidgets = widgets
        .filterNot { it.type == WidgetType.CLOCK }
        .take(2)

    return copy(widgets = listOf(clockWidget) + nonClockWidgets)
}
