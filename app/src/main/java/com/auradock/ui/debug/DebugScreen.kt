package com.auradock.ui.debug

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import com.auradock.analytics.ActivationSummary
import com.auradock.analytics.DOCKED_DAYS_NEEDED
import com.auradock.analytics.DOCKED_DAYS_WINDOW
import com.auradock.ui.components.BackArrow
import com.auradock.ui.theme.AuraDockColors
import com.auradock.ui.theme.AuraDockRadii
import com.auradock.ui.theme.AuraDockSpacing
import com.auradock.ui.theme.AuraDockTypography
import com.auradock.ui.theme.fromScreenTop
import java.util.Date

/**
 * The hidden debug page (tap the version label seven times in Setup): the two activation numbers worked out from the local event
 * log, and how often each event happened. Stateless; everything on it stays on this phone.
 */
@Composable
fun DebugScreen(summary: ActivationSummary?, version: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    fun at(millis: Long?): String = millis?.let { DateFormat.getMediumDateFormat(context).format(Date(it)) + " " + DateFormat.getTimeFormat(context).format(Date(it)) } ?: "not yet"
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AuraDockColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(start = AuraDockSpacing.mdl, end = AuraDockSpacing.mdl, top = fromScreenTop(AuraDockSpacing.xxl), bottom = AuraDockSpacing.xxl),
        verticalArrangement = Arrangement.spacedBy(AuraDockSpacing.sm)
    ) {
        BackArrow(Modifier.offset(x = -AuraDockSpacing.sm))
        Column(modifier = Modifier.padding(start = AuraDockSpacing.sm, end = AuraDockSpacing.sm, bottom = AuraDockSpacing.md)) {
            Text(text = "Debug", color = AuraDockColors.OnBackground, style = AuraDockTypography.StepTitle)
            Text(text = "AuraDock $version. Counted on this phone only.", color = AuraDockColors.OnSurfaceVariant, style = AuraDockTypography.Secondary, modifier = Modifier.padding(top = AuraDockSpacing.sm))
        }
        if (summary == null) {
            Text(text = "Loading…", color = AuraDockColors.OnSurfaceVariant, style = AuraDockTypography.Secondary, modifier = Modifier.padding(horizontal = AuraDockSpacing.sm))
        } else {
            Metric(
                title = "Activated within 24 h",
                value = if (summary.activatedWithin24h) "Yes" else "No",
                lines = listOf("Screensaver set: ${at(summary.screensaverSetAt)}", "First dock: ${at(summary.firstDockAt)}")
            )
            Metric(
                title = "Docked on $DOCKED_DAYS_NEEDED+ of the last $DOCKED_DAYS_WINDOW days",
                value = if (summary.dockedFiveOfSeven) "Yes" else "No",
                lines = listOf("${summary.dockedDays} of $DOCKED_DAYS_WINDOW days")
            )
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(AuraDockRadii.card)).background(AuraDockColors.Surface).padding(AuraDockSpacing.md),
                verticalArrangement = Arrangement.spacedBy(AuraDockSpacing.sm)
            ) {
                Text(text = "Events (${summary.total})", style = AuraDockTypography.CardTitle, color = AuraDockColors.OnBackground)
                if (summary.counts.isEmpty()) Text(text = "None yet.", style = AuraDockTypography.Meta, color = AuraDockColors.OnSurfaceVariant)
                summary.counts.forEach { (name, count) ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(text = name, style = AuraDockTypography.Meta, color = AuraDockColors.OnBackground, modifier = Modifier.weight(1f))
                        Text(text = count.toString(), style = AuraDockTypography.Meta, color = AuraDockColors.OnSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun Metric(title: String, value: String, lines: List<String>) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(AuraDockRadii.card)).background(AuraDockColors.Surface).padding(AuraDockSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AuraDockSpacing.xs)
    ) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text(text = title, style = AuraDockTypography.CardTitle, color = AuraDockColors.OnBackground, modifier = Modifier.weight(1f))
            Text(text = value, style = AuraDockTypography.Section, color = if (value == "Yes") AuraDockColors.Accent else AuraDockColors.OnSurfaceVariant)
        }
        lines.forEach { Text(text = it, style = AuraDockTypography.Meta, color = AuraDockColors.OnSurfaceVariant) }
    }
}
