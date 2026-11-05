package com.auradock.dock

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        modifier = modifier.padding(horizontal = 48.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        lines.forEach { line ->
            Text(
                text = line,
                color = if (dimmed) Color(0xFF3F3F3F) else Color(0xFF727272),
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
