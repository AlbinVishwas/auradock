package com.auradock.dock.split

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.auradock.ui.theme.AuraDockColors
import com.auradock.ui.theme.AuraDockSplit
import com.auradock.ui.theme.AuraDockTypography
import kotlin.math.roundToInt

/** A sample track so the Now playing thumbnail can be drawn (in the edit list and the Add widget sheet) when nothing is playing. */
val SampleNowPlaying = NowPlayingState(
    title = "Slow Morning", artist = "Kavya Rao", art = null, appIcon = null, appName = "Music", playing = false,
    durationMs = 180_000L, positionMs = 68_400L, positionAt = 0L, speed = 1f
)

/** A widget drawn small: the real widget at 405 x 364, scaled down into a thumbnail. */
@Composable
fun WidgetThumb(kind: WidgetKind, env: WidgetEnv, width: Dp, height: Dp, scale: Float, radius: Dp, modifier: Modifier = Modifier) {
    val sampleEnv = if (kind == WidgetKind.NOW_PLAYING && env.state.split.nowPlaying == null)
        env.copy(state = env.state.copy(split = env.state.split.copy(nowPlaying = SampleNowPlaying))) else env
    Box(
        modifier.size(width, height).clip(RoundedCornerShape(radius)).background(AuraDockColors.Background)
            .border(1.dp, AuraDockColors.QuietPill, RoundedCornerShape(radius))
            .clipToBounds()
    ) {
        // The widget is laid out at its full size from the thumbnail's top-left corner, then scaled down about that corner.
        Box(
            Modifier
                .layout { measurable, _ ->
                    val p = measurable.measure(androidx.compose.ui.unit.Constraints.fixed(AuraDockSplit.HalfWidth.roundToPx(), AuraDockSplit.HalfHeight.roundToPx()))
                    layout(0, 0) { p.place(0, 0) }
                }
                .graphicsLayer { scaleX = scale; scaleY = scale; transformOrigin = TransformOrigin(0f, 0f) }
        ) { SplitWidget(kind, sampleEnv) }
    }
}

/**
 * The edit list for one half's stack (frame SplitDock-Edit): drag a row's handle to reorder, the minus badge removes a widget, "Add widget"
 * opens the sheet, "Done" saves. The panel covers the half being edited; the other half drops to 25%.
 */
@Composable
fun SplitEditPanel(
    side: SplitSide,
    draft: List<WidgetKind>,
    showingNow: WidgetKind?,
    env: WidgetEnv,
    onChange: (List<WidgetKind>) -> Unit,
    onAdd: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rowPx = with(LocalDensity.current) { AuraDockSplit.EditRow.toPx() }
    var dragging by remember { mutableIntStateOf(-1) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    Column(
        modifier.size(AuraDockSplit.EditPanelWidth, AuraDockSplit.EditPanelHeight)
            .clip(RoundedCornerShape(AuraDockSplit.EditPanelRadius)).background(AuraDockColors.SplitCard)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(Modifier.fillMaxWidth().height(36.dp).padding(bottom = 0.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(if (side == SplitSide.LEFT) "Left stack" else "Right stack", style = AuraDockTypography.SplitEditTitle, color = AuraDockColors.DockText)
            Box(
                Modifier.height(36.dp).clip(RoundedCornerShape(18.dp)).background(AuraDockColors.Accent).clickable(role = Role.Button, onClick = onDone).padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) { Text("Done", style = AuraDockTypography.SplitEditDone, color = AuraDockColors.OnAccent) }
        }
        Spacer(Modifier.height(2.dp))
        draft.forEachIndexed { index, kind ->
            androidx.compose.runtime.key(kind) {
                val lifted = dragging == index
                Row(
                    Modifier.fillMaxWidth().height(AuraDockSplit.EditRow)
                        .graphicsLayer { translationY = if (lifted) dragOffset else 0f; if (lifted) { scaleX = 1.02f; scaleY = 1.02f } }
                        .background(if (lifted) AuraDockColors.QuietPill else AuraDockColors.SplitCard, RoundedCornerShape(10.dp)),
                    horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.DragHandle, contentDescription = "Drag to reorder ${kind.label}", tint = AuraDockColors.Tertiary,
                        modifier = Modifier.size(18.dp).pointerInput(kind, draft) {
                            detectDragGestures(
                                onDragStart = { dragging = draft.indexOf(kind); dragOffset = 0f },
                                onDrag = { change, amount ->
                                    change.consume()
                                    dragOffset += amount.y
                                    val steps = (dragOffset / rowPx).roundToInt()
                                    val from = dragging
                                    val to = (from + steps).coerceIn(0, draft.lastIndex)
                                    if (from >= 0 && to != from) {
                                        onChange(moveWidget(draft, from, to))
                                        dragging = to
                                        dragOffset -= (to - from) * rowPx
                                    }
                                },
                                onDragEnd = { dragging = -1; dragOffset = 0f },
                                onDragCancel = { dragging = -1; dragOffset = 0f }
                            )
                        }
                    )
                    WidgetThumb(kind, env, AuraDockSplit.EditThumbWidth, AuraDockSplit.EditThumbHeight, AuraDockSplit.EditThumbScale, 10.dp)
                    Column(Modifier.weight(1f)) {
                        Text(kind.label, style = AuraDockTypography.SplitEditRow, color = AuraDockColors.DockText)
                        Text(if (kind == showingNow) "Showing now" else kind.blurb, style = AuraDockTypography.SplitEditRowSub, color = AuraDockColors.Tertiary, modifier = Modifier.padding(top = 2.dp), maxLines = 1)
                    }
                    Box(
                        Modifier.size(AuraDockSplit.EditBadge).clip(CircleShape).background(AuraDockColors.Accent)
                            .alpha(if (draft.size > 1) 1f else 0.35f)
                            .semantics { contentDescription = "Remove ${kind.label}" }
                            .clickable(enabled = draft.size > 1, role = Role.Button) { onChange(removeWidget(draft, kind)) },
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Rounded.Remove, contentDescription = null, tint = AuraDockColors.OnAccent, modifier = Modifier.size(12.dp)) }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Row(
            Modifier.fillMaxWidth().height(AuraDockSplit.EditButton).clip(RoundedCornerShape(22.dp)).background(AuraDockColors.QuietPill).clickable(role = Role.Button, onClick = onAdd),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null, tint = AuraDockColors.DockText, modifier = Modifier.size(16.dp))
            Text("Add widget", style = AuraDockTypography.SplitEditAdd, color = AuraDockColors.DockText)
        }
    }
}

/**
 * "Add widget" (frame SplitDock-AddWidget): a sheet over a 60% scrim, the widgets in their groups (Time, Date, Info, App) as thumbnails with
 * their names. Tap one to add it to the stack being edited. The App widget is listed as "Coming soon".
 */
@Composable
fun SplitAddSheet(sideName: String, inStack: List<WidgetKind>, offered: List<WidgetKind>, env: WidgetEnv, onPick: (WidgetKind) -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().background(AuraDockColors.Background.copy(alpha = AuraDockSplit.SheetScrim)).clickable(enabled = true, indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, onClick = onDismiss)) {
        Column(
            Modifier.align(Alignment.BottomCenter).width(AuraDockSplit.SheetWidth).height(AuraDockSplit.SheetHeight)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)).background(AuraDockColors.SplitCard)
                .clickable(enabled = true, indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) {}
                .padding(start = 24.dp, end = 24.dp, top = 10.dp, bottom = 20.dp)
        ) {
            Box(Modifier.align(Alignment.CenterHorizontally).size(32.dp, 4.dp).clip(RoundedCornerShape(2.dp)).background(AuraDockColors.SheetHandle))
            Row(Modifier.padding(top = 12.dp, bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
                Text("Add widget", style = AuraDockTypography.SplitSheetTitle, color = AuraDockColors.DockText)
                Text("to the $sideName stack", style = AuraDockTypography.SplitEditRowSub, color = AuraDockColors.Tertiary)
            }
            val groups = WidgetGroup.entries
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                groups.chunked(2).forEach { pair ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                        pair.forEach { group -> Box(Modifier.weight(1f)) { GroupTiles(group, inStack, offered, env, onPick) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupTiles(group: WidgetGroup, inStack: List<WidgetKind>, offered: List<WidgetKind>, env: WidgetEnv, onPick: (WidgetKind) -> Unit) {
    Column {
        Text(group.label, style = AuraDockTypography.SplitSheetGroup, color = AuraDockColors.Tertiary, modifier = Modifier.padding(bottom = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (group == WidgetGroup.APP) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        Modifier.size(AuraDockSplit.SheetTileWidth, AuraDockSplit.SheetTileHeight).clip(RoundedCornerShape(12.dp)).background(AuraDockColors.Background)
                            .border(1.dp, AuraDockColors.QuietPill, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
        Text("Coming soon", style = AuraDockTypography.SplitBadge, color = AuraDockColors.DockSecondary, maxLines = 1, softWrap = false,
                            modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(AuraDockColors.QuietPill).padding(horizontal = 3.dp, vertical = 4.dp))
                    }
                    Text("App widget", style = AuraDockTypography.SplitSheetTile, color = AuraDockColors.Tertiary)
                }
            } else {
                // Widgets on the other side are not offered at all; those already in this stack are shown dimmed.
                WidgetKind.entries.filter { it.group == group && (it in offered || it in inStack) }.forEach { kind ->
                    val added = kind in inStack
                    Column(
                        Modifier.alpha(if (added) AuraDockSplit.DimAddOpacity else 1f).clickable(enabled = !added, role = Role.Button) { onPick(kind) },
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        WidgetThumb(kind, env, AuraDockSplit.SheetTileWidth, AuraDockSplit.SheetTileHeight, AuraDockSplit.SheetTileScale, 12.dp)
                        Text(kind.label, style = AuraDockTypography.SplitSheetTile, color = AuraDockColors.DockSecondary)
                    }
                }
            }
        }
    }
}
