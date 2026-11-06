package com.auradock.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.auradock.ui.components.QuietPill
import com.auradock.ui.theme.AuraDockColors
import com.auradock.ui.theme.AuraDockRadii
import com.auradock.ui.theme.AuraDockSizes
import com.auradock.ui.theme.AuraDockSpacing
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
 * The style guide's permission card: an icon disc, the title, a one-line reason and an Allow pill. Once allowed, the pill
 * becomes a "✓ Allowed" label in the accent.
 */
@Composable
fun PermissionCard(
    permission: SetupPermission,
    granted: Boolean,
    onAllow: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = AuraDockColors.Surface
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AuraDockRadii.card))
            .background(containerColor)
            .padding(AuraDockSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AuraDockSpacing.smd)
    ) {
        Box(
            modifier = Modifier.size(AuraDockSizes.iconDisc).clip(CircleShape).background(AuraDockColors.Field),
            contentAlignment = Alignment.Center
        ) {
            Text(text = permission.title.first().toString(), style = AuraDockTypography.Button, color = AuraDockColors.OnBackground)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = permission.title, style = AuraDockTypography.Button, color = AuraDockColors.OnBackground)
            Text(text = permission.why, style = AuraDockTypography.Caption, color = AuraDockColors.OnSurfaceVariant)
        }
        if (granted) Text(text = "✓ Allowed", style = AuraDockTypography.Caption, color = AuraDockColors.Accent)
        else QuietPill(text = "Allow", onClick = onAllow)
    }
}
