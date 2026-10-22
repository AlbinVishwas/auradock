package com.auradock.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.auradock.data.CalendarEvent
import com.auradock.data.NotificationItem
import com.auradock.ui.template.StandbyTemplate
import com.auradock.ui.template.TemplateRenderer
import com.auradock.ui.viewmodel.CalendarPermissionUiState

@Composable
fun StandbyScreen(
    template: StandbyTemplate,
    timeMillis: Long,
    todayEvents: List<CalendarEvent> = emptyList(),
    calendarPermissionState: CalendarPermissionUiState = CalendarPermissionUiState.DENIED,
    importantNotifications: List<NotificationItem> = emptyList(),
    hasNotificationAccess: Boolean = false,
    onEnableCalendarAccess: () -> Unit = {},
    onOpenNotificationSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.98f,
        animationSpec = tween(durationMillis = 420),
        label = "standbyScale"
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(durationMillis = 320))
    ) {
        TemplateRenderer(
            template = template,
            timeMillis = timeMillis,
            todayEvents = todayEvents,
            calendarPermissionState = calendarPermissionState,
            importantNotifications = importantNotifications,
            hasNotificationAccess = hasNotificationAccess,
            onEnableCalendarAccess = onEnableCalendarAccess,
            onOpenNotificationSettings = onOpenNotificationSettings,
            modifier = modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
        )
    }
}
