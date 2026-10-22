package com.auradock.ui.screens.dashboard

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.auradock.data.CalendarEvent
import com.auradock.data.NotificationItem
import com.auradock.standby.StandbyTriggerMode
import com.auradock.ui.components.QuickActionButton
import com.auradock.time.formatDashboardDate
import com.auradock.ui.components.ScaledPreview
import com.auradock.ui.components.StandbyPreviewCard
import com.auradock.ui.components.TodaySnapshotCard
import com.auradock.ui.template.StandbyTemplate
import com.auradock.ui.template.TemplateDefaults
import com.auradock.ui.template.TemplateRenderer
import com.auradock.ui.theme.AuraDockTheme
import com.auradock.ui.theme.LocalAuraDockSpacing
import com.auradock.ui.viewmodel.CalendarPermissionUiState
import com.auradock.ui.viewmodel.VoiceState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    currentTemplate: StandbyTemplate,
    timeMillis: Long,
    modifier: Modifier = Modifier,
    todayEvents: List<CalendarEvent> = emptyList(),
    calendarPermissionState: CalendarPermissionUiState = CalendarPermissionUiState.DENIED,
    importantNotifications: List<NotificationItem> = emptyList(),
    hasNotificationAccess: Boolean = false,
    standbyTriggerMode: StandbyTriggerMode = StandbyTriggerMode.CHARGING_AND_LANDSCAPE,
    isGenerating: Boolean = false,
    voiceState: VoiceState = VoiceState.Idle,
    voiceFeedbackText: String = "",
    partialTranscript: String = "",
    onCustomizeStandby: () -> Unit = {},
    onVoiceCommand: () -> Unit = {},
    onStandbyTriggerModeClick: () -> Unit = {},
    onEnableCalendarAccess: () -> Unit = {},
    onOpenNotificationSettings: () -> Unit = {}
) {
    val spacing = LocalAuraDockSpacing.current
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(voiceState) {
        if (voiceState == VoiceState.Success) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    val dateText = formatDashboardDate(timeMillis)
    val pulseTransition = rememberInfiniteTransition(label = "voicePulse")
    val micScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "micScale"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = spacing.lg),
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Good Morning, Albin",
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

        TodaySnapshotCard(
            modifier = Modifier.fillMaxWidth(),
            items = listOf(
                "Team standup 10:30",
                "Investor call 14:00",
                "Ship beta build"
            )
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
            QuickActionButton(
                text = when (standbyTriggerMode) {
                    StandbyTriggerMode.CHARGING_AND_LANDSCAPE -> "Charging + Landscape"
                    StandbyTriggerMode.CHARGING_ONLY -> "Charging Only"
                    StandbyTriggerMode.LANDSCAPE_ONLY -> "Landscape Only"
                },
                onClick = onStandbyTriggerModeClick
            )
            QuickActionButton(
                text = when (voiceState) {
                    VoiceState.Listening -> "Listening..."
                    VoiceState.Processing -> "Creating..."
                    VoiceState.Success -> "Done"
                    VoiceState.Error -> "Try Again"
                    VoiceState.Idle -> "Voice Command"
                },
                onClick = onVoiceCommand,
                isActive = voiceState == VoiceState.Listening || voiceState == VoiceState.Processing,
                modifier = Modifier.graphicsLayer {
                    val listening = voiceState == VoiceState.Listening
                    scaleX = if (listening) micScale else 1f
                    scaleY = if (listening) micScale else 1f
                }
            )
        }

        Spacer(modifier = Modifier.height(spacing.lg))

        StandbyPreviewCard(
            selected = true,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Lay the template out at a realistic landscape size, then shrink it to fit the card.
            ScaledPreview(designWidth = 800.dp) {
                TemplateRenderer(
                    template = currentTemplate,
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
        DashboardScreen(currentTemplate = TemplateDefaults.Split, timeMillis = System.currentTimeMillis())
    }
}
