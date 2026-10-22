package com.auradock.ui.template

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.auradock.data.CalendarEvent
import com.auradock.data.NotificationItem
import com.auradock.ui.components.GlassCard
import com.auradock.ui.theme.LocalAuraDockSpacing
import com.auradock.ui.viewmodel.CalendarPermissionUiState
import com.auradock.ui.widgets.CalendarWidget
import com.auradock.ui.widgets.ClockWidget
import com.auradock.ui.widgets.WeatherUiState
import com.auradock.ui.widgets.WeatherWidget

sealed class LayoutSlot {
    data object Full : LayoutSlot()
    data object Left : LayoutSlot()
    data object Right : LayoutSlot()
    data object Top : LayoutSlot()
    data object Bottom : LayoutSlot()
}

fun WidgetPosition.toLayoutSlot(): LayoutSlot = when (this) {
    WidgetPosition.FULL -> LayoutSlot.Full
    WidgetPosition.LEFT -> LayoutSlot.Left
    WidgetPosition.RIGHT -> LayoutSlot.Right
    WidgetPosition.TOP -> LayoutSlot.Top
    WidgetPosition.BOTTOM -> LayoutSlot.Bottom
}

@Composable
fun TemplateRenderer(
    template: StandbyTemplate,
    todayEvents: List<CalendarEvent> = emptyList(),
    calendarPermissionState: CalendarPermissionUiState = CalendarPermissionUiState.DENIED,
    importantNotifications: List<NotificationItem> = emptyList(),
    hasNotificationAccess: Boolean = false,
    onEnableCalendarAccess: () -> Unit = {},
    onOpenNotificationSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val spacing = LocalAuraDockSpacing.current
    val renderPlan = createRenderPlan(template)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(spacing.xl)
    ) {
        when (renderPlan.layoutType) {
            LayoutType.MINIMAL -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    RenderWidget(
                        widget = renderPlan.widgets.firstOrNull(),
                        todayEvents = todayEvents,
                        calendarPermissionState = calendarPermissionState,
                        importantNotifications = importantNotifications,
                        hasNotificationAccess = hasNotificationAccess,
                        onEnableCalendarAccess = onEnableCalendarAccess,
                        onOpenNotificationSettings = onOpenNotificationSettings,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            LayoutType.SPLIT -> {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.md)
                ) {
                    RenderWidget(
                        widget = renderPlan.widgets.getOrNull(0),
                        todayEvents = todayEvents,
                        calendarPermissionState = calendarPermissionState,
                        importantNotifications = importantNotifications,
                        hasNotificationAccess = hasNotificationAccess,
                        onEnableCalendarAccess = onEnableCalendarAccess,
                        onOpenNotificationSettings = onOpenNotificationSettings,
                        modifier = Modifier.weight(1f)
                    )
                    RenderWidget(
                        widget = renderPlan.widgets.getOrNull(1),
                        todayEvents = todayEvents,
                        calendarPermissionState = calendarPermissionState,
                        importantNotifications = importantNotifications,
                        hasNotificationAccess = hasNotificationAccess,
                        onEnableCalendarAccess = onEnableCalendarAccess,
                        onOpenNotificationSettings = onOpenNotificationSettings,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            LayoutType.FOCUS -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    RenderWidget(
                        widget = renderPlan.widgets.firstOrNull(),
                        todayEvents = todayEvents,
                        calendarPermissionState = calendarPermissionState,
                        importantNotifications = importantNotifications,
                        hasNotificationAccess = hasNotificationAccess,
                        onEnableCalendarAccess = onEnableCalendarAccess,
                        onOpenNotificationSettings = onOpenNotificationSettings,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            LayoutType.GRID -> {
                // GRID is normalized to a safe split render path for balanced distribution.
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.md)
                ) {
                    RenderWidget(
                        widget = renderPlan.widgets.getOrNull(0),
                        todayEvents = todayEvents,
                        calendarPermissionState = calendarPermissionState,
                        importantNotifications = importantNotifications,
                        hasNotificationAccess = hasNotificationAccess,
                        onEnableCalendarAccess = onEnableCalendarAccess,
                        onOpenNotificationSettings = onOpenNotificationSettings,
                        modifier = Modifier.weight(1f)
                    )
                    RenderWidget(
                        widget = renderPlan.widgets.getOrNull(1),
                        todayEvents = todayEvents,
                        calendarPermissionState = calendarPermissionState,
                        importantNotifications = importantNotifications,
                        hasNotificationAccess = hasNotificationAccess,
                        onEnableCalendarAccess = onEnableCalendarAccess,
                        onOpenNotificationSettings = onOpenNotificationSettings,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun RenderWidget(
    widget: WidgetConfig?,
    todayEvents: List<CalendarEvent>,
    calendarPermissionState: CalendarPermissionUiState,
    importantNotifications: List<NotificationItem>,
    hasNotificationAccess: Boolean,
    onEnableCalendarAccess: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(widget?.type) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(durationMillis = 320)) +
            slideInVertically(
                initialOffsetY = { it / 12 },
                animationSpec = tween(durationMillis = 420)
            )
    ) {
        GlassCard(modifier = modifier.fillMaxSize()) {
            when (widget?.type) {
                WidgetType.CLOCK -> ClockWidget(timeMillis = System.currentTimeMillis(), modifier = Modifier.fillMaxSize())
                WidgetType.WEATHER -> WeatherWidget(state = defaultWeatherState(), modifier = Modifier.fillMaxSize())
                WidgetType.CALENDAR -> CalendarWidget(
                    todayEvents = todayEvents,
                    calendarPermissionState = calendarPermissionState,
                    onEnableCalendarAccess = onEnableCalendarAccess,
                    modifier = Modifier.fillMaxSize()
                )
                WidgetType.REMINDERS -> ReminderWidget(
                    notifications = importantNotifications,
                    hasNotificationAccess = hasNotificationAccess,
                    onOpenNotificationSettings = onOpenNotificationSettings,
                    modifier = Modifier.fillMaxSize()
                )
                null -> ClockWidget(timeMillis = System.currentTimeMillis(), modifier = Modifier.fillMaxSize())
            }
        }
    }
}

private data class RenderPlan(
    val templateId: String,
    val layoutType: LayoutType,
    val widgets: List<WidgetConfig>
)

private fun createRenderPlan(template: StandbyTemplate): RenderPlan {
    val fallback = safeFallbackPlan()

    if (template.widgets.isEmpty()) return fallback

    val base = template.sanitizedForStandby()
    val clock = base.widgets.firstOrNull { it.type == WidgetType.CLOCK } ?: return fallback

    // Strict constraints:
    // - max 2 widgets + clock (enforced by sanitizedForStandby)
    // - clock always present
    // - no overflow or empty balanced layouts
    if (base.widgets.size > 3) return fallback

    val slotMap = base.widgets.associateBy { it.position.toLayoutSlot() }
    val firstSecondary = base.widgets.firstOrNull { it.type != WidgetType.CLOCK }

    return when (base.layoutType) {
        LayoutType.MINIMAL -> RenderPlan(
            templateId = base.id,
            layoutType = LayoutType.MINIMAL,
            widgets = listOf(clock)
        )

        LayoutType.SPLIT -> {
            val left = slotMap[LayoutSlot.Left] ?: clock
            val rightCandidate = slotMap[LayoutSlot.Right] ?: firstSecondary ?: defaultWeatherWidget(WidgetPosition.RIGHT)
            val right = if (rightCandidate.type == WidgetType.CLOCK) {
                defaultWeatherWidget(WidgetPosition.RIGHT)
            } else {
                rightCandidate
            }

            RenderPlan(
                templateId = base.id,
                layoutType = LayoutType.SPLIT,
                widgets = listOf(left, right)
            )
        }

        LayoutType.FOCUS -> {
            val focusWidget = slotMap[LayoutSlot.Full] ?: slotMap[LayoutSlot.Top] ?: clock
            if (focusWidget.type == WidgetType.CLOCK) {
                RenderPlan(
                    templateId = base.id,
                    layoutType = LayoutType.FOCUS,
                    widgets = listOf(clock)
                )
            } else {
                // Keep clock always present by safely normalizing to split.
                RenderPlan(
                    templateId = base.id,
                    layoutType = LayoutType.SPLIT,
                    widgets = listOf(clock, focusWidget)
                )
            }
        }

        LayoutType.GRID -> {
            val top = slotMap[LayoutSlot.Top] ?: clock
            val bottomCandidate = slotMap[LayoutSlot.Bottom] ?: firstSecondary ?: defaultWeatherWidget(WidgetPosition.BOTTOM)
            val bottom = if (bottomCandidate.type == WidgetType.CLOCK) {
                defaultWeatherWidget(WidgetPosition.BOTTOM)
            } else {
                bottomCandidate
            }

            // Normalize GRID to split to guarantee balanced halves.
            RenderPlan(
                templateId = base.id,
                layoutType = LayoutType.SPLIT,
                widgets = listOf(top, bottom)
            )
        }
    }
}

private fun safeFallbackPlan() = RenderPlan(
    templateId = "safe_fallback",
    layoutType = LayoutType.SPLIT,
    widgets = listOf(
        WidgetConfig(type = WidgetType.CLOCK, position = WidgetPosition.LEFT),
        WidgetConfig(type = WidgetType.WEATHER, position = WidgetPosition.RIGHT)
    )
)

private fun defaultWeatherWidget(position: WidgetPosition) = WidgetConfig(
    type = WidgetType.WEATHER,
    position = position
)

private fun defaultWeatherState() = WeatherUiState(
    temperatureLabel = "22°",
    condition = "Partly Cloudy",
    cityName = "BANGALORE",
    iconRes = android.R.drawable.ic_menu_compass
)
