package com.auradock.ui.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auradock.data.CalendarEvent
import com.auradock.ui.components.QuickActionButton
import com.auradock.ui.theme.AuraDockColors
import com.auradock.ui.theme.AuraDockTypography
import com.auradock.ui.viewmodel.CalendarPermissionUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CalendarWidget(
    todayEvents: List<CalendarEvent>,
    calendarPermissionState: CalendarPermissionUiState,
    onEnableCalendarAccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formatter = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "TODAY",
            style = AuraDockTypography.Caption.copy(
                color = AuraDockColors.OnSurfaceVariant,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.SemiBold
            )
        )

        if (calendarPermissionState != CalendarPermissionUiState.GRANTED) {
            Text(
                text = "Calendar permission needed",
                style = AuraDockTypography.WidgetLabel,
                color = AuraDockColors.OnSurfaceVariant
            )
            Text(
                text = if (calendarPermissionState == CalendarPermissionUiState.PERMANENTLY_DENIED) {
                    "Permission denied permanently"
                } else {
                    "Enable calendar access to see your schedule"
                },
                style = AuraDockTypography.Caption,
                color = AuraDockColors.OnSurfaceVariant
            )
            QuickActionButton(
                text = if (calendarPermissionState == CalendarPermissionUiState.PERMANENTLY_DENIED) {
                    "Open Settings"
                } else {
                    "Enable Calendar Access"
                },
                onClick = onEnableCalendarAccess
            )
            return
        }

        if (todayEvents.isEmpty()) {
            Text(
                text = "No events today",
                style = AuraDockTypography.WidgetLabel,
                color = Color.White
            )
            Text(
                text = "You are all clear",
                style = AuraDockTypography.Caption,
                color = AuraDockColors.OnSurfaceVariant
            )
            return
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            todayEvents.take(2).forEach { event ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = event.title,
                        style = AuraDockTypography.WidgetLabel,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = if (event.allDay) "All day" else formatter.format(Date(event.startTime)),
                        style = AuraDockTypography.Caption,
                        color = AuraDockColors.OnSurfaceVariant
                    )
                }
            }
        }

        val nextEvent = todayEvents.firstOrNull()
        val nextText = if (nextEvent == null) {
            ""
        } else {
            if (nextEvent.allDay) "Next: All day" else "Next: ${formatter.format(Date(nextEvent.startTime))}"
        }
        Text(
            text = nextText,
            style = AuraDockTypography.Caption,
            color = AuraDockColors.OnSurfaceVariant
        )
    }
}
