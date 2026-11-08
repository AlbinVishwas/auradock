package com.auradock.dock

import com.auradock.ui.theme.AuraDockTypography
import com.auradock.ui.theme.AuraDockColors
import com.auradock.ui.theme.AuraDockDockLayout
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
 * "6 held · 2 through": how many notifications Do Not Disturb silenced during this session and how many reached the user (a
 * picked app, or someone Do Not Disturb lets through). Counts only, never what a notification said. Null when both are zero,
 * so a quiet session shows nothing.
 */
fun heldCaption(held: Int, through: Int): String? =
    if (held <= 0 && through <= 0) null else "${held.coerceAtLeast(0)} held · ${through.coerceAtLeast(0)} through"

/**
 * The notification strip along the bottom of every template: small, grey and single-line, so the clock stays the
 * loudest thing. It draws nothing when [lines] is empty. It sits inside the dock content, so it drifts with it
 * ([com.auradock.ui.components.BurnInProtection]) and no pixel stays lit.
 */
@Composable
fun NotificationStrip(lines: List<String>, dimmed: Boolean, modifier: Modifier = Modifier, caption: String? = null) {
    if (lines.isEmpty() && caption == null) return
    val stripColor = if (dimmed) AuraDockColors.StripTextDim else AuraDockColors.StripText
    val captionColor = if (dimmed) AuraDockColors.HeldCaptionDim else AuraDockColors.HeldCaption
    Column(
        modifier = modifier.padding(horizontal = AuraDockDockLayout.stripPadding, vertical = AuraDockDockLayout.stripVertical),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // The held-for-you counts sit next to the last line of the strip (on a line of their own when there is no strip), a step quieter,
        // so they take no extra row.
        lines.forEachIndexed { i, line ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = line, color = stripColor, style = AuraDockTypography.DockStrip, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                if (caption != null && i == lines.lastIndex) {
                    Text(text = caption, color = captionColor, style = AuraDockTypography.DockStrip, maxLines = 1, modifier = Modifier.padding(start = AuraDockDockLayout.captionGap))
                }
            }
        }
        if (lines.isEmpty() && caption != null) Text(text = caption, color = captionColor, style = AuraDockTypography.DockStrip, maxLines = 1)
    }
}
