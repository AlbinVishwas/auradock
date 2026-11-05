package com.auradock.dock

import com.auradock.ui.theme.AuraDockTypography
import com.auradock.ui.theme.AuraDockColors
import com.auradock.ui.theme.AuraDockDockLayout
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.auradock.data.NotificationItem

/**
 * One quiet line for a notification: "App · who". The message text is only added when the user turned that on,
 * because a docked phone sits where other people can read it.
 */
fun stripLine(item: NotificationItem, showText: Boolean): String {
    val head = "${item.appName} · ${item.title}"
    val text = item.content.trim()
    return if (showText && text.isNotEmpty()) "$head: $text" else head
}

/** The lines the strip shows: nothing at all (so no strip) when there are no notifications. */
fun stripLines(items: List<NotificationItem>, showText: Boolean): List<String> = items.map { stripLine(it, showText) }

/**
 * The notification strip along the bottom of every template: small, grey and single-line, so the clock stays the
 * loudest thing. It draws nothing when [lines] is empty. It sits inside the dock content, so it drifts with it
 * ([com.auradock.ui.components.BurnInProtection]) and no pixel stays lit.
 */
@Composable
fun NotificationStrip(lines: List<String>, dimmed: Boolean, modifier: Modifier = Modifier) {
    if (lines.isEmpty()) return
    Column(
        modifier = modifier.padding(horizontal = AuraDockDockLayout.stripPadding, vertical = AuraDockDockLayout.stripVertical),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        lines.forEach { line ->
            Text(
                text = line,
                color = if (dimmed) AuraDockColors.StripTextDim else AuraDockColors.StripText,
                style = AuraDockTypography.DockStrip,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
