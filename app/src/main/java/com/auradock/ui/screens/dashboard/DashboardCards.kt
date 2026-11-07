package com.auradock.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DesktopWindows
import androidx.compose.material.icons.outlined.ViewCarousel
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.graphics.vector.ImageVector
import com.auradock.data.CalendarEvent
import com.auradock.dock.DockContent
import com.auradock.dock.DockTemplate
import com.auradock.dock.DockUiState
import com.auradock.ui.components.LocalDockMorph
import com.auradock.ui.components.PrimaryPill
import com.auradock.ui.components.ScaledPreview
import com.auradock.ui.components.reportBounds
import com.auradock.ui.theme.AuraDockColors
import com.auradock.ui.theme.AuraDockGallery
import com.auradock.ui.theme.AuraDockRadii
import com.auradock.ui.theme.AuraDockSizes
import com.auradock.ui.theme.AuraDockSpacing
import com.auradock.ui.theme.AuraDockTypography
import com.auradock.ui.viewmodel.CalendarPermissionUiState

/** A dashboard card: 24 dp corners on the card colour. */
@Composable
private fun SectionCard(modifier: Modifier = Modifier, padding: androidx.compose.foundation.layout.PaddingValues, content: @Composable () -> Unit) {
    Column(modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(AuraDockRadii.card)).background(AuraDockColors.Surface).padding(padding)) { content() }
}

/**
 * The "Calendar" card (handoff Main / Empty / NoCalendar). Events as a time column and a title; the next one says how soon it is.
 * Without access: a calendar tile, one line and an Allow pill. The data has no end time or place yet, so there is no
 * "30 min · Zoom" line under a title.
 */
@Composable
fun CalendarCard(
    events: List<CalendarEvent>?,
    permission: CalendarPermissionUiState,
    nowMillis: Long,
    formatTime: (Long) -> String,
    onEnableAccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val inset = AuraDockSpacing.mdl
    if (permission != CalendarPermissionUiState.GRANTED) {
        SectionCard(modifier, androidx.compose.foundation.layout.PaddingValues(inset)) {
            Text(text = "Calendar", style = AuraDockTypography.Section, color = AuraDockColors.OnBackground)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = AuraDockSpacing.md + AuraDockSpacing.xxs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AuraDockSpacing.md - AuraDockSpacing.xxs)
            ) {
                Box(
                    modifier = Modifier.size(AuraDockSizes.calendarTileSize).clip(RoundedCornerShape(AuraDockRadii.calendarTile)).background(AuraDockColors.IconTile),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Outlined.CalendarToday, contentDescription = null, tint = AuraDockColors.OnBackground, modifier = Modifier.size(AuraDockSizes.factIcon)) }
                Text(
                    text = if (permission == CalendarPermissionUiState.PERMANENTLY_DENIED) "Calendar access is turned off for AuraDock" else "Allow calendar to see today's events",
                    style = AuraDockTypography.Secondary,
                    color = AuraDockColors.OnSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .height(AuraDockSizes.calendarAllow)
                        .clip(RoundedCornerShape(AuraDockSizes.calendarAllow / 2))
                        .background(AuraDockColors.Field)
                        .clickable(role = Role.Button, onClick = onEnableAccess)
                        .padding(horizontal = AuraDockSpacing.mdl - AuraDockSpacing.xxs),
                    contentAlignment = Alignment.Center
                ) { Text(text = if (permission == CalendarPermissionUiState.PERMANENTLY_DENIED) "Settings" else "Allow", style = AuraDockTypography.PillSmall, color = AuraDockColors.OnBackground) }
            }
        }
        return
    }
    SectionCard(modifier, androidx.compose.foundation.layout.PaddingValues(start = inset, end = inset, top = inset, bottom = AuraDockSpacing.smd - AuraDockSpacing.xxs)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Text(text = "Calendar", style = AuraDockTypography.Section, color = AuraDockColors.OnBackground)
            if (!events.isNullOrEmpty()) Text(text = if (events.size == 1) "1 event" else "${events.size} events", style = AuraDockTypography.Meta, color = AuraDockColors.Tertiary)
        }
        if (events != null && events.isEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = AuraDockSpacing.sm - AuraDockSpacing.xxs, bottom = AuraDockSpacing.smd),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "No events today", style = AuraDockTypography.Secondary, color = AuraDockColors.OnSurfaceVariant)
                Icon(Icons.Outlined.CalendarToday, contentDescription = null, tint = AuraDockColors.Tertiary, modifier = Modifier.size(AuraDockSizes.icon))
            }
        } else if (events != null) {
            val next = events.firstOrNull { !it.allDay && it.startTime >= nowMillis }
            Column(modifier = Modifier.padding(top = AuraDockSpacing.xs)) {
                events.forEachIndexed { index, event ->
                    if (index > 0) Box(Modifier.fillMaxWidth().height(AuraDockSizes.line).background(AuraDockColors.RowDivider))
                    EventRow(event, formatTime, soon = if (event == next) untilLabel(event.startTime - nowMillis) else null)
                }
            }
        }
    }
}

@Composable
private fun EventRow(event: CalendarEvent, formatTime: (Long) -> String, soon: String?) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = AuraDockSpacing.md - AuraDockSpacing.xxs),
        horizontalArrangement = Arrangement.spacedBy(AuraDockSpacing.md),
        verticalAlignment = Alignment.Top
    ) {
        Text(text = eventTime(event, formatTime), color = AuraDockColors.OnBackground, softWrap = false, maxLines = 1, modifier = Modifier.width(AuraDockSizes.timeColumn))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = event.title, style = AuraDockTypography.Body, color = AuraDockColors.OnBackground)
            if (soon != null) Text(text = soon, style = AuraDockTypography.Caption, color = AuraDockColors.Accent, modifier = Modifier.padding(top = AuraDockSpacing.xxs + AuraDockSpacing.hairline))
        }
    }
}

/** "5:30 PM" as a big "5:30" and a small "PM"; a 24-hour time or "All day" has nothing to split. */
@Composable
private fun eventTime(event: CalendarEvent, formatTime: (Long) -> String): AnnotatedString {
    if (event.allDay) return buildAnnotatedString { withStyle(AuraDockTypography.Caption.toSpanStyle()) { append("All day") } }
    val parts = formatTime(event.startTime).trim().split(Regex("\\s+"))
    return buildAnnotatedString {
        withStyle(AuraDockTypography.EventTime.toSpanStyle()) { append(parts.first()) }
        if (parts.size > 1) withStyle(AuraDockTypography.EventMeridiem.toSpanStyle().copy(color = AuraDockColors.OnSurfaceVariant)) { append(" " + parts.last()) }
    }
}

/** "In 40 min", "In 2 h"; null once it is under a minute away. */
private fun untilLabel(millis: Long): String? {
    val minutes = (millis / 60_000L).toInt()
    return when {
        minutes < 1 -> null
        minutes < 60 -> "In $minutes min"
        minutes % 60 == 0 -> "In ${minutes / 60} h"
        else -> "In ${minutes / 60} h ${minutes % 60} min"
    }
}

/**
 * The dock card: a thumbnail of the dock in use, its name with a "Change" link, and the one primary pill. The pill's bounds are
 * reported so the Dock now morph can grow from it.
 */
@Composable
fun DockCard(
    template: DockTemplate,
    dockState: DockUiState,
    onChange: () -> Unit,
    onDockNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(AuraDockRadii.card)).background(AuraDockColors.Surface).padding(AuraDockSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AuraDockSpacing.smd + AuraDockSpacing.xxs)
    ) {
        val shape = RoundedCornerShape(AuraDockRadii.thumbnail)
        Box(
            modifier = Modifier.fillMaxWidth().height(AuraDockSizes.thumbnailHeight).clip(shape).background(AuraDockColors.Background).border(AuraDockSizes.line, AuraDockColors.Field, shape)
        ) {
            // Lay the template out at a realistic landscape size, then shrink it to fit the card.
            ScaledPreview(designWidth = AuraDockGallery.previewDesignWidth, modifier = Modifier.fillMaxWidth().height(AuraDockSizes.thumbnailHeight)) {
                DockContent(template = template, state = dockState)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = AuraDockSpacing.xs),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = template.label, style = AuraDockTypography.Button, color = AuraDockColors.OnBackground)
                Text(text = "Opens when charging on a stand", style = AuraDockTypography.RowCaption, color = AuraDockColors.OnSurfaceVariant, modifier = Modifier.padding(top = AuraDockSpacing.xxs))
            }
            Text(
                text = "Change",
                style = AuraDockTypography.Meta,
                color = AuraDockColors.OnSurfaceVariant,
                modifier = Modifier.clickable(role = Role.Button, onClick = onChange).padding(horizontal = AuraDockSpacing.xs, vertical = AuraDockSpacing.smd + AuraDockSpacing.xxs)
            )
        }
        val morph = LocalDockMorph.current
        var pillBounds by remember { mutableStateOf(Rect.Zero) }
        PrimaryPill(
            text = "Dock now",
            icon = Icons.Outlined.DesktopWindows,
            onClick = { morph(pillBounds, onDockNow) },
            modifier = Modifier.reportBounds { pillBounds = it }
        )
    }
}

/** Templates and Focus: two outlined tiles side by side, each with an icon, a name and a line under it. */
@Composable
fun NavTiles(templateCount: Int, onOpenTemplates: () -> Unit, onOpenFocus: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AuraDockSpacing.smd)) {
        NavTile(Icons.Outlined.ViewCarousel, "Templates", "$templateCount docks", onOpenTemplates, Modifier.weight(1f))
        NavTile(Icons.Outlined.DarkMode, "Focus", "While docked", onOpenFocus, Modifier.weight(1f))
    }
}

@Composable
private fun NavTile(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(AuraDockRadii.row)
    Column(
        modifier = modifier.clip(shape).border(AuraDockSizes.line, AuraDockColors.TileOutline, shape).clickable(role = Role.Button, onClick = onClick).padding(AuraDockSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AuraDockSpacing.smd - AuraDockSpacing.xxs)
    ) {
        Icon(icon, contentDescription = null, tint = AuraDockColors.OnBackground, modifier = Modifier.size(AuraDockSizes.navTileIcon))
        Column {
            Text(text = title, style = AuraDockTypography.Body, color = AuraDockColors.OnBackground)
            Text(text = subtitle, style = AuraDockTypography.RowCaption, color = AuraDockColors.OnSurfaceVariant, modifier = Modifier.padding(top = AuraDockSpacing.xxs))
        }
    }
}
