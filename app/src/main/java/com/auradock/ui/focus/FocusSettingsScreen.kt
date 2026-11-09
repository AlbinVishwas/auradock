package com.auradock.ui.focus

import androidx.compose.foundation.Image
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.auradock.ui.components.BackArrow
import com.auradock.ui.components.PrimaryPill
import com.auradock.ui.theme.AuraDockGallery
import com.auradock.ui.theme.AuraDockMotion
import com.auradock.ui.theme.fromScreenBottom
import com.auradock.ui.theme.fromScreenTop
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
import androidx.compose.ui.draw.clip
import com.auradock.ui.components.AuraCheckbox
import com.auradock.ui.onboarding.PermissionCard
import com.auradock.ui.onboarding.SetupPermission
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
    val overlayGranted: Boolean,
    val vip: VipUiState = VipUiState()
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
    val vip: VipActions = VipActions(),
    /** Loads an app's icon; null (no icon) is fine, the row then shows a blank. */
    val loadIcon: suspend (String) -> ImageBitmap? = { null }
)

/**
 * Focus settings (handoff Focus, AppPicker and AppPicker-Limit). Stateless apart from whether the app picker is showing, which
 * belongs to this screen alone. Draws [state] and reports taps through [actions].
 */
@Composable
fun FocusSettingsScreen(
    state: FocusSettingsUiState,
    actions: FocusSettingsActions,
    modifier: Modifier = Modifier,
    /** Null where nothing is wired to open Setup yet (the Focus route): the row is then left out. */
    onOpenSetup: (() -> Unit)? = null
) {
    var picking by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = picking) { picking = false }
    if (picking) AppPicker(state, actions, onDone = { picking = false }, modifier = modifier)
    else FocusPage(state, actions, onChooseApps = { picking = true }, onOpenSetup = onOpenSetup, modifier = modifier)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FocusPage(state: FocusSettingsUiState, actions: FocusSettingsActions, onChooseApps: () -> Unit, onOpenSetup: (() -> Unit)?, modifier: Modifier) {
    val picked = state.apps.orEmpty().filter { it.packageName in state.pickedApps }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AuraDockColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(start = AuraDockSpacing.mdl, end = AuraDockSpacing.mdl, top = fromScreenTop(AuraDockSpacing.xxl), bottom = AuraDockSpacing.xxl - AuraDockSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(AuraDockSpacing.smd + AuraDockSpacing.xxs)
    ) {
        BackArrow(Modifier.offset(x = -AuraDockSpacing.sm))
        Text(
            text = "Focus",
            color = AuraDockColors.OnBackground,
            style = AuraDockTypography.StepTitle,
            modifier = Modifier.padding(start = AuraDockSpacing.sm, end = AuraDockSpacing.sm, bottom = AuraDockSpacing.smd - AuraDockSpacing.xxs)
        )

        FocusCard {
            SwitchRow(
                title = "Focus while docked",
                body = "Do Not Disturb turns on when you dock. Your Favourites and the people below can still call.",
                checked = state.focusWhileDocked,
                onChange = actions.onToggleFocus
            )
            if (!state.hasDndAccess) {
                AccessRow(Icons.Outlined.DarkMode, "Do Not Disturb access", "Not allowed yet, needed for focus", actions.onOpenDndSettings)
            }
        }

        FocusCard {
            Text(text = "Apps that can reach you", style = AuraDockTypography.CardTitle, color = AuraDockColors.OnBackground)
            Text(
                text = "Up to $MAX_PICKED_APPS. Everything else waits.",
                style = AuraDockTypography.Meta,
                color = AuraDockColors.OnSurfaceVariant,
                modifier = Modifier.padding(top = AuraDockSpacing.xs)
            )
            if (picked.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.padding(top = AuraDockSpacing.smd + AuraDockSpacing.xxs),
                    horizontalArrangement = Arrangement.spacedBy(AuraDockSpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(AuraDockSpacing.sm)
                ) { picked.forEach { AppChip(it, actions.loadIcon) } }
            }
            Divider(Modifier.padding(top = AuraDockSpacing.smd + AuraDockSpacing.xxs))
            Row(
                modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onChooseApps).padding(vertical = AuraDockSpacing.smd + AuraDockSpacing.xxs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Choose apps", style = AuraDockTypography.RowLabel, color = AuraDockColors.OnBackground, modifier = Modifier.weight(1f))
                Text(text = "${state.pickedApps.size} of $MAX_PICKED_APPS", style = AuraDockTypography.Meta, color = AuraDockColors.OnSurfaceVariant)
                Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = AuraDockColors.Tertiary, modifier = Modifier.padding(start = AuraDockSpacing.smd).size(AuraDockSizes.rowChevron))
            }
            if (!state.hasNotificationAccess) {
                AccessRow(Icons.Outlined.ChatBubbleOutline, "Notification access", "Needed to show their messages", actions.onOpenNotificationAccess)
            }
        }

        VipCard(state.vip, actions.vip)

        FocusCard {
            SwitchRow(
                title = "Show message text on dock",
                body = "Off by default. The dock shows only the app and sender, for privacy.",
                checked = state.showMessageText,
                onChange = actions.onToggleMessageText
            )
        }

        // Not in the handoff's Focus page: the switch for the pill that starts the dock from any app.
        DockFromAnywhereCard(state.dockFromAnywhere, state.overlayGranted, actions.onToggleDockFromAnywhere, actions.onAllowOverlay)

        if (onOpenSetup != null) {
            val shape = RoundedCornerShape(AuraDockRadii.row)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .border(AuraDockSizes.line, AuraDockColors.TileOutline, shape)
                    .clickable(role = Role.Button, onClick = onOpenSetup)
                    .padding(horizontal = AuraDockSpacing.mdl - AuraDockSpacing.xxs, vertical = AuraDockSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AuraDockSpacing.smd)
            ) {
                Icon(Icons.Outlined.Shield, contentDescription = null, tint = AuraDockColors.OnBackground, modifier = Modifier.size(AuraDockSizes.iconSmall))
                Text(text = "Setup and permissions", style = AuraDockTypography.RowLabel, color = AuraDockColors.OnBackground, modifier = Modifier.weight(1f))
                Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = AuraDockColors.Tertiary, modifier = Modifier.size(AuraDockSizes.rowChevron))
            }
        }
    }
}

/** A card with the Focus page's padding: 18 dp, a little less underneath where a row brings its own. */
@Composable
private fun FocusCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AuraDockRadii.card))
            .background(AuraDockColors.Surface)
            .padding(AuraDockSpacing.mdl - AuraDockSpacing.xxs)
    ) { content() }
}

@Composable
private fun Divider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(AuraDockSizes.line).background(AuraDockColors.RowDivider))
}

/** A title and a grey line on the left, a switch on the right. */
@Composable
private fun SwitchRow(title: String, body: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(AuraDockSpacing.md), verticalAlignment = Alignment.Top) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = AuraDockTypography.CardTitle, color = AuraDockColors.OnBackground)
            Text(text = body, style = AuraDockTypography.Meta, color = AuraDockColors.OnSurfaceVariant, modifier = Modifier.padding(top = AuraDockSpacing.xs))
        }
        AuraSwitch(checked = checked, onCheckedChange = onChange)
    }
}

/** "Do Not Disturb access: Not allowed yet" with an Allow pill, under a divider. */
@Composable
private fun AccessRow(icon: ImageVector, title: String, body: String, onAllow: () -> Unit) {
    Divider(Modifier.padding(top = AuraDockSpacing.md))
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = AuraDockSpacing.smd + AuraDockSpacing.xxs, bottom = AuraDockSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AuraDockSpacing.smd)
    ) {
        Icon(icon, contentDescription = null, tint = AuraDockColors.OnBackground, modifier = Modifier.size(AuraDockSizes.factIcon))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = AuraDockTypography.RowLabel, color = AuraDockColors.OnBackground)
            Text(text = body, style = AuraDockTypography.RowCaption, color = AuraDockColors.OnSurfaceVariant, modifier = Modifier.padding(top = AuraDockSpacing.xxs))
        }
        Box(
            modifier = Modifier
                .height(AuraDockSizes.smallPill)
                .clip(RoundedCornerShape(AuraDockSizes.smallPill / 2))
                .background(AuraDockColors.QuietPill)
                .clickable(role = Role.Button, onClick = onAllow)
                .padding(horizontal = AuraDockSpacing.md),
            contentAlignment = Alignment.Center
        ) { Text(text = "Allow", style = AuraDockTypography.SmallAction, color = AuraDockColors.OnBackground) }
    }
}

/** A picked app: its tile (the real icon, or its first letter) and its name, on a 40 dp pill. */
@Composable
private fun AppChip(app: AppEntry, loadIcon: suspend (String) -> ImageBitmap?) {
    Row(
        modifier = Modifier
            .height(AuraDockSizes.appChip)
            .clip(RoundedCornerShape(AuraDockSizes.appChip / 2))
            .background(AuraDockColors.Field)
            .padding(start = AuraDockSpacing.xs + AuraDockSpacing.xxs, end = AuraDockSpacing.smd + AuraDockSpacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AuraDockSpacing.sm)
    ) {
        AppTile(app, loadIcon, AuraDockSizes.appTile, AuraDockRadii.chipTile, AuraDockTypography.TileLetter, AuraDockColors.Inactive)
        Text(text = app.label, style = AuraDockTypography.ChipLabel, color = AuraDockColors.OnBackground)
    }
}

/** The app's icon, or while it loads (or without one) its first letter on a grey tile. */
@Composable
private fun AppTile(app: AppEntry, loadIcon: suspend (String) -> ImageBitmap?, size: androidx.compose.ui.unit.Dp, radius: androidx.compose.ui.unit.Dp, letter: androidx.compose.ui.text.TextStyle, background: Color, dimmed: Boolean = false) {
    val icon by produceState<ImageBitmap?>(initialValue = null, app.packageName) { value = loadIcon(app.packageName) }
    val image = icon
    val shape = RoundedCornerShape(radius)
    if (image != null) {
        Image(bitmap = image, contentDescription = null, modifier = Modifier.size(size).clip(shape), alpha = if (dimmed) 0.4f else 1f)
    } else {
        Box(Modifier.size(size).clip(shape).background(background), contentAlignment = Alignment.Center) {
            Text(text = app.label.take(1).uppercase(), style = letter, color = AuraDockColors.OnBackground)
        }
    }
}

/**
 * Choose up to three apps (handoff AppPicker): a search bar, a row per app with a round tick, and Done. At the limit the rows
 * that are not picked dim to 35% and cannot be tapped; untick one to swap.
 */
@Composable
private fun AppPicker(state: FocusSettingsUiState, actions: FocusSettingsActions, onDone: () -> Unit, modifier: Modifier) {
    var query by rememberSaveable { mutableStateOf("") }
    val all = state.apps
    val shown = all?.filter { query.isBlank() || it.label.contains(query.trim(), ignoreCase = true) }
    val atLimit = state.pickedApps.size >= MAX_PICKED_APPS
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AuraDockColors.Background)
            .padding(start = AuraDockSpacing.mdl, end = AuraDockSpacing.mdl, top = fromScreenTop(AuraDockSpacing.xxl), bottom = fromScreenBottom(AuraDockSpacing.xxl - AuraDockSpacing.sm)),
        verticalArrangement = Arrangement.spacedBy(AuraDockSpacing.smd)
    ) {
        BackArrow(Modifier.offset(x = -AuraDockSpacing.sm), onBack = onDone)
        Column(modifier = Modifier.padding(horizontal = AuraDockSpacing.sm)) {
            Text(text = "Choose apps", color = AuraDockColors.OnBackground, style = AuraDockTypography.StepTitle)
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = AuraDockColors.OnBackground, fontWeight = FontWeight.SemiBold)) { append("${state.pickedApps.size} of $MAX_PICKED_APPS selected") }
                    append(if (atLimit) " · Unselect one to swap" else " · Pick up to $MAX_PICKED_APPS")
                },
                style = AuraDockTypography.Secondary,
                color = AuraDockColors.OnSurfaceVariant,
                modifier = Modifier.padding(top = AuraDockSpacing.sm)
            )
        }
        Box(
            modifier = Modifier
                .padding(top = AuraDockSpacing.xs + AuraDockSpacing.xxs)
                .fillMaxWidth()
                .height(AuraDockSizes.searchBar)
                .clip(RoundedCornerShape(AuraDockSizes.searchBar / 2))
                .background(AuraDockColors.Surface)
                .padding(horizontal = AuraDockSpacing.md),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                textStyle = AuraDockTypography.Secondary.copy(color = AuraDockColors.OnBackground),
                cursorBrush = SolidColor(AuraDockColors.OnBackground),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) Text(text = "Search apps", style = AuraDockTypography.Secondary, color = AuraDockColors.Tertiary)
                        inner()
                    }
                }
            )
        }
        if (shown == null) {
            Text(text = "Loading apps…", color = AuraDockColors.OnSurfaceVariant, style = AuraDockTypography.Secondary, modifier = Modifier.weight(1f))
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(shown, key = { it.packageName }) { app ->
                    val picked = app.packageName in state.pickedApps
                    val enabled = canToggle(state.pickedApps, app.packageName)
                    AppRow(app, picked, enabled, actions.loadIcon, onClick = { actions.onToggleApp(app.packageName) })
                }
            }
        }
        PrimaryPill(text = "Done", onClick = onDone)
    }
}

@Composable
private fun AppRow(app: AppEntry, picked: Boolean, enabled: Boolean, loadIcon: suspend (String) -> ImageBitmap?, onClick: () -> Unit) {
    val alpha by animateFloatAsState(if (enabled) 1f else AuraDockGallery.DimmedAlpha, AuraDockMotion.tween(AuraDockMotion.Fast), label = "rowDim")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(AuraDockSizes.appRow)
            .graphicsLayer { this.alpha = alpha }
            .clickable(enabled = enabled, role = Role.Checkbox, onClick = onClick)
            .padding(horizontal = AuraDockSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AuraDockSpacing.smd + AuraDockSpacing.xxs)
    ) {
        AppTile(app, loadIcon, AuraDockSizes.iconDisc, AuraDockRadii.iconTile, AuraDockTypography.TileLetterLarge, AuraDockColors.Field, dimmed = false)
        Text(text = app.label, style = AuraDockTypography.Body, color = AuraDockColors.OnBackground, modifier = Modifier.weight(1f))
        AuraCheckbox(checked = picked)
    }
}

@Composable
internal fun SettingsCard(content: @Composable () -> Unit) {
    Surface(shape = RoundedCornerShape(AuraDockRadii.card), color = AuraDockColors.Surface, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(AuraDockSpacing.mdl - AuraDockSpacing.xxs)) { content() }
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
                style = AuraDockTypography.CardTitle,
                color = AuraDockColors.OnBackground,
                modifier = Modifier.weight(1f)
            )
            AuraSwitch(checked = enabled, onCheckedChange = onToggle)
        }
        Spacer(Modifier.height(AuraDockSpacing.xs))
        Text(
            text = "Optional. When you set the phone sideways and still, a small pill appears over any app so you can start " +
                "the dock without opening AuraDock. It stays out of the way while media plays or you are on a call. " +
                "This needs \"Display over other apps\", and AuraDock shows a small permanent notification while it is on.",
            style = AuraDockTypography.Meta,
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
