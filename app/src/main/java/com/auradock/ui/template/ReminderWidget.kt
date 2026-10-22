package com.auradock.ui.template

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auradock.data.NotificationItem
import com.auradock.ui.components.GlassCard
import com.auradock.ui.components.QuickActionButton
import com.auradock.ui.theme.AuraDockColors

@Composable
fun ReminderWidget(
    notifications: List<NotificationItem>,
    hasNotificationAccess: Boolean,
    onOpenNotificationSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "REMINDERS",
                color = AuraDockColors.OnSurfaceVariant,
                letterSpacing = 2.sp,
                fontSize = 11.sp
            )
            if (!hasNotificationAccess) {
                Text(
                    text = "Enable notification access",
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    fontSize = 18.sp
                )
                QuickActionButton(
                    text = "Open Settings",
                    onClick = onOpenNotificationSettings
                )
            } else if (notifications.isEmpty()) {
                Text(
                    text = "No important alerts",
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    fontSize = 20.sp
                )
                Text(
                    text = "Notification listener idle",
                    color = AuraDockColors.OnSurfaceVariant,
                    fontSize = 12.sp
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    notifications.take(2).forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = item.appName,
                                color = AuraDockColors.OnSurfaceVariant,
                                fontSize = 11.sp
                            )
                            Text(
                                text = item.title,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                Text(
                    text = "Showing important notifications",
                    color = AuraDockColors.OnSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }
    }
}
