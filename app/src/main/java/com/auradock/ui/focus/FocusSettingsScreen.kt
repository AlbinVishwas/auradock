package com.auradock.ui.focus

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auradock.focus.AppEntry
import com.auradock.focus.MAX_PICKED_APPS
import com.auradock.focus.canToggle
import com.auradock.ui.components.QuickActionButton
import com.auradock.ui.theme.AuraDockColors
import com.auradock.ui.theme.AuraDockTypography

private val Amber = Color(0xFFFFB300)

/** The Focus settings page, opened from the dashboard: Do Not Disturb while docked, and which apps show on the dock. */
@Composable
fun FocusSettingsScreen(
    focusWhileDocked: Boolean,
    hasDndAccess: Boolean,
    onToggleFocus: (Boolean) -> Unit,
    onOpenDndSettings: () -> Unit,
    hasNotificationAccess: Boolean,
    onOpenNotificationAccess: () -> Unit,
    apps: List<AppEntry>?,
    pickedApps: List<String>,
    onToggleApp: (String) -> Unit,
    loadIcon: suspend (String) -> ImageBitmap?,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(text = "Focus", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
        }

        item {
            SettingsCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Focus while docked",
                        style = AuraDockTypography.WidgetPrimary,
                        color = AuraDockColors.OnBackground,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(checked = focusWhileDocked, onCheckedChange = onToggleFocus)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "While the dock is showing, Do Not Disturb silences everything except alarms and calls from your " +
                        "starred contacts. When the dock closes, your own Do Not Disturb settings are put back exactly as " +
                        "they were. If Do Not Disturb is already on, AuraDock leaves it alone.",
                    style = AuraDockTypography.Caption,
                    color = AuraDockColors.OnSurfaceVariant
                )
                if (!hasDndAccess) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "AuraDock needs Do Not Disturb access to do this (newer Android calls it \"Modes access\").",
                        style = AuraDockTypography.WidgetLabel,
                        color = Amber
                    )
                    Spacer(Modifier.height(8.dp))
                    QuickActionButton(text = "Allow Do Not Disturb access", onClick = onOpenDndSettings, isActive = true)
                } else if (focusWhileDocked) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Ready: Do Not Disturb turns on when you dock.",
                        style = AuraDockTypography.Caption,
                        color = AuraDockColors.OnSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        item {
            SettingsCard {
                Text(
                    text = "Notifications on the dock",
                    style = AuraDockTypography.WidgetPrimary,
                    color = AuraDockColors.OnBackground
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Choose up to $MAX_PICKED_APPS apps. The dock shows their latest notification as a small line, " +
                        "so you can see what matters without picking up the phone.",
                    style = AuraDockTypography.Caption,
                    color = AuraDockColors.OnSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "${pickedApps.size} of $MAX_PICKED_APPS chosen",
                    style = AuraDockTypography.WidgetLabel,
                    color = if (pickedApps.size == MAX_PICKED_APPS) AuraDockColors.ClockGlow else AuraDockColors.OnSurfaceVariant
                )
                if (!hasNotificationAccess) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "AuraDock needs notification access to read them.",
                        style = AuraDockTypography.WidgetLabel,
                        color = Amber
                    )
                    Spacer(Modifier.height(8.dp))
                    QuickActionButton(text = "Allow notification access", onClick = onOpenNotificationAccess, isActive = true)
                }
            }
            Spacer(Modifier.height(4.dp))
        }

        if (apps == null) {
            item { Text(text = "Loading apps…", color = AuraDockColors.OnSurfaceVariant, modifier = Modifier.padding(8.dp)) }
        } else {
            items(apps, key = { it.packageName }) { app ->
                val picked = app.packageName in pickedApps
                val enabled = canToggle(pickedApps, app.packageName)
                AppRow(app, picked, enabled, loadIcon, onClick = { onToggleApp(app.packageName) })
            }
        }
    }
}

@Composable
private fun AppRow(app: AppEntry, picked: Boolean, enabled: Boolean, loadIcon: suspend (String) -> ImageBitmap?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIcon(app.packageName, loadIcon, dimmed = !enabled)
        Spacer(Modifier.size(16.dp))
        Text(
            text = app.label,
            color = if (enabled) Color.White else Color(0xFF5E5E5E),
            fontSize = 18.sp,
            modifier = Modifier.weight(1f)
        )
        Checkbox(checked = picked, onCheckedChange = null, enabled = enabled)
    }
}

@Composable
private fun AppIcon(packageName: String, loadIcon: suspend (String) -> ImageBitmap?, dimmed: Boolean) {
    val icon by produceState<ImageBitmap?>(initialValue = null, packageName) { value = loadIcon(packageName) }
    val image = icon
    if (image != null) {
        Image(
            bitmap = image,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            alpha = if (dimmed) 0.4f else 1f
        )
    } else {
        Box(Modifier.size(40.dp))
    }
}

@Composable
internal fun SettingsCard(content: @Composable () -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), color = AuraDockColors.Surface, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) { content() }
    }
}
