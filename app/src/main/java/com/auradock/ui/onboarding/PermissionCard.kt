package com.auradock.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.defaultMinSize
import com.auradock.ui.theme.AuraDockColors
import com.auradock.ui.theme.AuraDockRadii
import com.auradock.ui.theme.AuraDockSizes
import com.auradock.ui.theme.AuraDockSpacing
import com.auradock.ui.theme.AuraDockTypography

/** Everything AuraDock can ask the user for, each with the one-line reason shown on its card (handoff v1 wording). */
enum class SetupPermission(val title: String, val why: String, val icon: ImageVector, val sheetTitle: String, val trust: String = "Nothing leaves this phone.") {
    CALENDAR("Calendar", "See today's events", Icons.Outlined.CalendarToday, "Show today's events?"),
    NOTIFICATION_ACCESS("Notification access", "Show messages from apps you choose", Icons.Outlined.ChatBubbleOutline, "Show messages on the dock?"),
    DND_ACCESS("Do Not Disturb access", "Silence distractions while docked", Icons.Outlined.DarkMode, "Silence distractions while docked?"),
    LOCATION("Location", "Local weather, approximate only", Icons.Outlined.LocationOn, "Show the local weather?", trust = "Only a rounded position goes to the weather service."),
    POST_NOTIFICATIONS("Notifications", "Remind you when tasks are due", Icons.Outlined.Notifications, "Remind you when this is due?"),
    EXACT_ALARMS("Alarms and reminders", "Remind you on time", Icons.Outlined.Alarm, "Remind you on the minute?"),
    CONTACTS("Contacts", "Choose people who can always reach you", Icons.Outlined.People, "Choose who can always reach you?")
}

/**
 * The permission row from the handoff: an icon tile, the title and a one-line reason, then either "Skip" and an Allow pill,
 * or, once allowed, a round check and the word "Allowed". [onSkip] is null where there is nothing to skip (the Setup page).
 */
@Composable
fun PermissionCard(
    permission: SetupPermission,
    granted: Boolean,
    onAllow: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = AuraDockColors.Surface,
    onSkip: (() -> Unit)? = null,
    skipped: Boolean = false,
    /** The Setup page's version: a status line under the reason, and "Manage" instead of the round check once allowed. */
    showStatus: Boolean = false
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AuraDockRadii.row))
            .background(containerColor)
            .padding(start = AuraDockSpacing.md, end = AuraDockSpacing.smd + AuraDockSpacing.xxs, top = AuraDockSpacing.smd + AuraDockSpacing.xxs, bottom = AuraDockSpacing.smd + AuraDockSpacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AuraDockSpacing.smd)
    ) {
        Box(
            modifier = Modifier.size(AuraDockSizes.iconDisc).clip(RoundedCornerShape(AuraDockRadii.iconTile)).background(AuraDockColors.IconTile),
            contentAlignment = Alignment.Center
        ) {
            Icon(permission.icon, contentDescription = null, tint = AuraDockColors.OnBackground, modifier = Modifier.size(AuraDockSizes.tileIcon))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = permission.title, style = AuraDockTypography.RowTitle, color = AuraDockColors.OnBackground)
            Text(text = permission.why, style = AuraDockTypography.RowCaption, color = AuraDockColors.OnSurfaceVariant, modifier = Modifier.padding(top = AuraDockSpacing.xxs))
            Text(text = permission.trust, style = AuraDockTypography.RowCaption, color = AuraDockColors.Tertiary, modifier = Modifier.padding(top = AuraDockSpacing.xxs))
            if (showStatus) {
                Row(modifier = Modifier.padding(top = AuraDockSpacing.sm - AuraDockSpacing.xxs), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AuraDockSpacing.xs)) {
                    if (granted) Icon(Icons.Rounded.Check, contentDescription = null, tint = AuraDockColors.OnBackground, modifier = Modifier.size(AuraDockSizes.statusIcon))
                    Text(
                        text = if (granted) "Allowed" else "Not allowed",
                        style = AuraDockTypography.SmallBadge,
                        color = if (granted) AuraDockColors.OnBackground else AuraDockColors.Accent
                    )
                }
            }
        }
        if (showStatus && granted) {
            Box(
                modifier = Modifier
                    .heightIn(min = AuraDockSizes.smallPill)
                    .clickable(role = Role.Button, onClick = onAllow)
                    .padding(horizontal = AuraDockSpacing.smd - AuraDockSpacing.xxs),
                contentAlignment = Alignment.Center
            ) { Text(text = "Manage", style = AuraDockTypography.RowCaption, color = AuraDockColors.OnSurfaceVariant) }
        } else if (granted) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AuraDockSpacing.sm - AuraDockSpacing.xxs)) {
                Box(Modifier.size(AuraDockSizes.allowedCheck).clip(CircleShape).background(AuraDockColors.Field), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = AuraDockColors.OnBackground, modifier = Modifier.size(AuraDockSizes.allowedCheckIcon))
                }
                Text(text = "Allowed", style = AuraDockTypography.RowCaption, color = AuraDockColors.OnSurfaceVariant)
            }
        } else if (skipped) {
            Text(text = "Skipped", style = AuraDockTypography.RowCaption, color = AuraDockColors.Tertiary)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AuraDockSpacing.xs)) {
                if (onSkip != null) {
                    Box(
                        modifier = Modifier
                            .heightIn(min = AuraDockSizes.smallPill)
                            .clickable(role = Role.Button, onClick = onSkip)
                            .padding(horizontal = AuraDockSpacing.sm),
                        contentAlignment = Alignment.Center
                    ) { Text(text = "Skip", style = AuraDockTypography.RowCaption, color = AuraDockColors.OnSurfaceVariant) }
                }
                Box(
                    modifier = Modifier
                        .heightIn(min = AuraDockSizes.smallPill)
                        .clip(RoundedCornerShape(AuraDockSizes.smallPill / 2))
                        .background(AuraDockColors.QuietPill)
                        .clickable(role = Role.Button, onClick = onAllow)
                        .padding(horizontal = AuraDockSpacing.md),
                    contentAlignment = Alignment.Center
                ) { Text(text = "Allow", style = AuraDockTypography.SmallAction, color = AuraDockColors.OnBackground) }
            }
        }
    }
}
