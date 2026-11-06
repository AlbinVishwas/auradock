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
import com.auradock.ui.components.AuraSwitch
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
import com.auradock.ui.components.QuietPill
import com.auradock.ui.theme.AuraDockColors
import com.auradock.ui.theme.AuraDockRadii
import com.auradock.ui.theme.AuraDockSizes
import com.auradock.ui.theme.AuraDockSpacing
import com.auradock.ui.theme.AuraDockTypography


/** The Focus settings page, opened from the dashboard: Do Not Disturb while docked, and which apps show on the dock. */
/** Everything Focus settings shows. */
data class FocusSettingsUiState(
    val focusWhileDocked: Boolean,
    val hasDndAccess: Boolean,
    val hasNotificationAccess: Boolean,
    /** Null while the app list is still loading. */
    val apps: List<AppEntry>?,
    val pickedApps: List<String>,
    val showMessageText: Boolean,
    val dockFromAnywhere: Boolean,
    val overlayGranted: Boolean
)

/** What the user can do in Focus settings. */
data class FocusSettingsActions(
    val onToggleFocus: (Boolean) -> Unit = {},
    val onOpenDndSettings: () -> Unit = {},
    val onOpenNotificationAccess: () -> Unit = {},
    val onToggleApp: (String) -> Unit = {},
    val onToggleMessageText: (Boolean) -> Unit = {},
    val onToggleDockFromAnywhere: (Boolean) -> Unit = {},
    val onAllowOverlay: () -> Unit = {},
    /** Loads an app's icon; null (no icon) is fine, the row then shows a blank. */
    val loadIcon: suspend (String) -> ImageBitmap? = { null }
)

/** Focus settings. Stateless: draws [state] and reports taps through [actions]. */
@Composable
fun FocusSettingsScreen(
    state: FocusSettingsUiState,
    actions: FocusSettingsActions,
    modifier: Modifier = Modifier
) {
    val focusWhileDocked = state.focusWhileDocked
    val hasDndAccess = state.hasDndAccess
    val hasNotificationAccess = state.hasNotificationAccess
    val apps = state.apps
    val pickedApps = state.pickedApps
    val showMessageText = state.showMessageText
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(AuraDockColors.Background),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = AuraDockSpacing.lg, vertical = AuraDockSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(AuraDockSpacing.sm)
    ) {
        item {
            Text(text = "Focus", color = AuraDockColors.OnBackground, style = AuraDockTypography.ScreenTitle)
            Spacer(Modifier.height(AuraDockSpacing.smd))
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
                    AuraSwitch(checked = focusWhileDocked, onCheckedChange = actions.onToggleFocus)
                }
                Spacer(Modifier.height(AuraDockSpacing.sm))
                Text(
                    text = "While the dock is showing, Do Not Disturb silences everything except alarms and calls from your " +
                        "starred contacts. When the dock closes, your own Do Not Disturb settings are put back exactly as " +
                        "they were. If Do Not Disturb is already on, AuraDock leaves it alone.",
                    style = AuraDockTypography.Caption,
                    color = AuraDockColors.OnSurfaceVariant
                )
                if (!hasDndAccess) {
                    Spacer(Modifier.height(AuraDockSpacing.md))
                    Text(
                        text = "AuraDock needs Do Not Disturb access to do this (newer Android calls it \"Modes access\").",
                        style = AuraDockTypography.WidgetLabel,
                        color = AuraDockColors.Accent
                    )
                    Spacer(Modifier.height(AuraDockSpacing.sm))
                    QuickActionButton(text = "Allow Do Not Disturb access", onClick = actions.onOpenDndSettings, isActive = true)
                } else if (focusWhileDocked) {
                    Spacer(Modifier.height(AuraDockSpacing.smd))
                    Text(
                        text = "Ready: Do Not Disturb turns on when you dock.",
                        style = AuraDockTypography.Caption,
                        color = AuraDockColors.OnSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(AuraDockSpacing.smd))
        }

        item {
            DockFromAnywhereCard(state.dockFromAnywhere, state.overlayGranted, actions.onToggleDockFromAnywhere, actions.onAllowOverlay)
            Spacer(Modifier.height(AuraDockSpacing.smd))
        }

        item {
            SettingsCard {
                Text(
                    text = "Notifications on the dock",
                    style = AuraDockTypography.WidgetPrimary,
                    color = AuraDockColors.OnBackground
                )
                Spacer(Modifier.height(AuraDockSpacing.sm))
                Text(
                    text = "Choose up to $MAX_PICKED_APPS apps. The dock shows their latest notification as a small line, " +
                        "so you can see what matters without picking up the phone.",
                    style = AuraDockTypography.Caption,
                    color = AuraDockColors.OnSurfaceVariant
                )
                Spacer(Modifier.height(AuraDockSpacing.smd))
                Text(
                    text = "${pickedApps.size} of $MAX_PICKED_APPS chosen",
                    style = AuraDockTypography.WidgetLabel,
                    color = if (pickedApps.size == MAX_PICKED_APPS) AuraDockColors.Accent else AuraDockColors.OnSurfaceVariant
                )
                Spacer(Modifier.height(AuraDockSpacing.smd))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Show message text on dock",
                        style = AuraDockTypography.WidgetLabel,
                        color = AuraDockColors.OnBackground,
                        modifier = Modifier.weight(1f)
                    )
                    AuraSwitch(checked = showMessageText, onCheckedChange = actions.onToggleMessageText)
                }
                Text(
                    text = "Off by default: the dock shows the app and who it is from, not what the message says.",
                    style = AuraDockTypography.Caption,
                    color = AuraDockColors.OnSurfaceVariant
                )
                if (!hasNotificationAccess) {
                    Spacer(Modifier.height(AuraDockSpacing.smd))
                    Text(
                        text = "AuraDock needs notification access to read them.",
                        style = AuraDockTypography.WidgetLabel,
                        color = AuraDockColors.Accent
                    )
                    Spacer(Modifier.height(AuraDockSpacing.sm))
                    QuickActionButton(text = "Allow notification access", onClick = actions.onOpenNotificationAccess, isActive = true)
                }
            }
            Spacer(Modifier.height(AuraDockSpacing.xs))
        }

        if (apps == null) {
            item { Text(text = "Loading apps…", color = AuraDockColors.OnSurfaceVariant, modifier = Modifier.padding(AuraDockSpacing.sm)) }
        } else {
            items(apps, key = { it.packageName }) { app ->
                val picked = app.packageName in pickedApps
                val enabled = canToggle(pickedApps, app.packageName)
                AppRow(app, picked, enabled, actions.loadIcon, onClick = { actions.onToggleApp(app.packageName) })
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
            .padding(AuraDockSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIcon(app.packageName, loadIcon, dimmed = !enabled)
        Spacer(Modifier.size(AuraDockSpacing.md))
        Text(
            text = app.label,
            color = if (enabled) AuraDockColors.OnBackground else AuraDockColors.OnDisabled,
            style = AuraDockTypography.Title,
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
            modifier = Modifier.size(AuraDockSizes.appIcon),
            alpha = if (dimmed) 0.4f else 1f
        )
    } else {
        Box(Modifier.size(AuraDockSizes.appIcon))
    }
}

@Composable
internal fun SettingsCard(content: @Composable () -> Unit) {
    Surface(shape = RoundedCornerShape(AuraDockRadii.card), color = AuraDockColors.Surface, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(AuraDockSpacing.mdl)) { content() }
    }
}

/** The "Dock from anywhere" switch with its honest explanation. Used in Focus settings and in onboarding. */
@Composable
fun DockFromAnywhereCard(
    enabled: Boolean,
    overlayGranted: Boolean,
    onToggle: (Boolean) -> Unit,
    onAllowOverlay: () -> Unit
) {
    SettingsCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Dock from anywhere",
                style = AuraDockTypography.WidgetPrimary,
                color = AuraDockColors.OnBackground,
                modifier = Modifier.weight(1f)
            )
            AuraSwitch(checked = enabled, onCheckedChange = onToggle)
        }
        Spacer(Modifier.height(AuraDockSpacing.sm))
        Text(
            text = "Optional. When you set the phone sideways and still, a small pill appears over any app so you can start " +
                "the dock without opening AuraDock. It stays out of the way while media plays or you are on a call. " +
                "This needs \"Display over other apps\", and AuraDock shows a small permanent notification while it is on.",
            style = AuraDockTypography.Caption,
            color = AuraDockColors.OnSurfaceVariant
        )
        if (enabled && !overlayGranted) {
            Spacer(Modifier.height(AuraDockSpacing.smd))
            Text(
                text = "Waiting for \"Display over other apps\".",
                style = AuraDockTypography.Caption,
                color = AuraDockColors.Accent
            )
            Spacer(Modifier.height(AuraDockSpacing.sm))
            QuietPill(text = "Allow display over other apps", onClick = onAllowOverlay)
        } else if (enabled) {
            Spacer(Modifier.height(AuraDockSpacing.smd))
            Text("On.", style = AuraDockTypography.Caption, color = AuraDockColors.OnSurfaceVariant)
        }
    }
}
