package com.auradock.ui.screens.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.util.Date
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auradock.data.CalendarEvent
import com.auradock.data.NotificationItem
import com.auradock.ui.components.QuickActionButton
import com.auradock.time.formatDashboardDate
import com.auradock.time.greetingFor
import com.auradock.ui.components.ScaledPreview
import com.auradock.ui.components.StandbyPreviewCard
import com.auradock.ui.components.TodaySnapshotCard
import com.auradock.dock.DockTemplate
import com.auradock.ui.template.toLegacyTemplate
import com.auradock.ui.template.TemplateRenderer
import com.auradock.ui.theme.AuraDockTheme
import com.auradock.ui.theme.LocalAuraDockSpacing
import com.auradock.ui.viewmodel.CalendarPermissionUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    currentTemplate: DockTemplate,
    timeMillis: Long,
    modifier: Modifier = Modifier,
    todayEvents: List<CalendarEvent> = emptyList(),
    calendarPermissionState: CalendarPermissionUiState = CalendarPermissionUiState.DENIED,
    importantNotifications: List<NotificationItem> = emptyList(),
    hasNotificationAccess: Boolean = false,
    onCustomizeStandby: () -> Unit = {},
    onEnableCalendarAccess: () -> Unit = {},
    onOpenNotificationSettings: () -> Unit = {}
) {
    val spacing = LocalAuraDockSpacing.current
    val context = LocalContext.current

    val dateText = formatDashboardDate(timeMillis)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = spacing.lg),
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = greetingFor(timeMillis),
            modifier = Modifier.padding(top = spacing.xl),
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.SemiBold
        )

        Text(
            text = dateText,
            color = Color.Gray,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = spacing.xs)
        )

        Spacer(modifier = Modifier.height(spacing.lg))

        val timeFormat = remember(context) { DateFormat.getTimeFormat(context) }
        val summary = buildTodaySummary(
            events = todayEvents,
            permission = calendarPermissionState,
            formatTime = { timeFormat.format(Date(it)) }
        )
        TodaySnapshotCard(
            modifier = Modifier.fillMaxWidth(),
            items = summary.lines,
            emptyStateText = summary.emptyText,
            actionText = summary.actionText,
            onAction = onEnableCalendarAccess
        )

        Spacer(modifier = Modifier.height(spacing.lg))

        // FlowRow wraps buttons onto a new line when they don't fit, so none is squeezed or pushed off-screen.
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            QuickActionButton(text = "Add Reminder", onClick = {})
            QuickActionButton(text = "Customize Standby", onClick = onCustomizeStandby)
        }

        Spacer(modifier = Modifier.height(spacing.lg))

        StandbyPreviewCard(
            selected = true,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Lay the template out at a realistic landscape size, then shrink it to fit the card.
            ScaledPreview(designWidth = 800.dp) {
                TemplateRenderer(
                    template = currentTemplate.toLegacyTemplate(),
                    timeMillis = timeMillis,
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

        Spacer(modifier = Modifier.height(spacing.xl))
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun DashboardScreenPreview() {
    AuraDockTheme {
        DashboardScreen(currentTemplate = DockTemplate.CLASSIC, timeMillis = System.currentTimeMillis())
    }
}
