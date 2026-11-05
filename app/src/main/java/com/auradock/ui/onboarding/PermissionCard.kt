package com.auradock.ui.onboarding

import com.auradock.ui.theme.AuraDockSpacing
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.auradock.ui.components.QuickActionButton
import com.auradock.ui.focus.SettingsCard
import com.auradock.ui.theme.AuraDockColors
import com.auradock.ui.theme.AuraDockTypography

/** Everything AuraDock can ask the user for, each with the one-line reason shown on its card. */
enum class SetupPermission(val title: String, val why: String) {
    CALENDAR("Calendar", "Shows today's events on the dashboard and the dock."),
    NOTIFICATION_ACCESS("Notification access", "Lets the dock show the latest message from the apps you pick."),
    DND_ACCESS("Do Not Disturb access", "Silences the phone while the dock is showing, then puts your settings back."),
    LOCATION("Location (approximate)", "Finds the weather for where you are; never precise, never stored."),
    POST_NOTIFICATIONS("Notifications", "Lets task reminders appear at their due time."),
    EXACT_ALARMS("Exact alarms", "Makes task reminders fire on the minute instead of a few minutes late.")
}

/**
 * One permission: why it is needed, a checkmark once granted, and "Allow" (plus "Skip" when [onSkip] is given, as in the
 * walkthrough). A skipped card keeps "Allow", so the user can still change their mind.
 */
@Composable
fun PermissionCard(
    permission: SetupPermission,
    granted: Boolean,
    onAllow: () -> Unit,
    modifier: Modifier = Modifier,
    skipped: Boolean = false,
    onSkip: (() -> Unit)? = null
) {
    SettingsCard {
        Row(
            modifier = modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = permission.title,
                style = AuraDockTypography.WidgetPrimary,
                color = AuraDockColors.OnBackground,
                modifier = Modifier.weight(1f)
            )
            if (granted) Text(text = "✓ Allowed", style = AuraDockTypography.WidgetLabel, color = AuraDockColors.Success)
            else if (skipped) Text(text = "Skipped", style = AuraDockTypography.WidgetLabel, color = AuraDockColors.OnSurfaceVariant)
        }
        Spacer(Modifier.height(AuraDockSpacing.sm))
        Text(text = permission.why, style = AuraDockTypography.Caption, color = AuraDockColors.OnSurfaceVariant)
        if (!granted) {
            Spacer(Modifier.height(AuraDockSpacing.smd))
            Row(horizontalArrangement = Arrangement.spacedBy(AuraDockSpacing.smd)) {
                QuickActionButton(text = "Allow", onClick = onAllow, isActive = true)
                if (onSkip != null && !skipped) QuickActionButton(text = "Skip", onClick = onSkip)
            }
        }
    }
}
