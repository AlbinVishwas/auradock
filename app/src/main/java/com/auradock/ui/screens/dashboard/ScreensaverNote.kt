package com.auradock.ui.screens.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import com.auradock.screensaver.ScreensaverBanner
import com.auradock.ui.theme.AuraDockColors
import com.auradock.ui.theme.AuraDockSpacing
import com.auradock.ui.theme.AuraDockTypography

/**
 * A quiet line about the screensaver test, under the weekly line: "AuraDock appeared on its own" once the screensaver has really
 * started (with "Done" to put it away), or, a day after install with no screensaver yet, a gentle hint with "Try again".
 */
@Composable
fun ScreensaverNote(banner: ScreensaverBanner, onDone: () -> Unit, onTryAgain: () -> Unit, modifier: Modifier = Modifier) {
    if (banner == ScreensaverBanner.NONE) return
    Row(
        modifier = modifier.fillMaxWidth().padding(top = AuraDockSpacing.smd),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AuraDockSpacing.smd)
    ) {
        if (banner == ScreensaverBanner.APPEARED) {
            Text(text = "✓ AuraDock appeared on its own", style = AuraDockTypography.Secondary, color = AuraDockColors.OnBackground, modifier = Modifier.weight(1f))
            Text(
                text = "Done",
                style = AuraDockTypography.SmallAction,
                color = AuraDockColors.OnSurfaceVariant,
                modifier = Modifier.clickable(role = Role.Button, onClick = onDone).padding(vertical = AuraDockSpacing.sm, horizontal = AuraDockSpacing.xs)
            )
        } else {
            Text(text = "The screensaver hasn't started yet.", style = AuraDockTypography.Secondary, color = AuraDockColors.OnSurfaceVariant, modifier = Modifier.weight(1f))
            Text(
                text = "Try again",
                style = AuraDockTypography.SmallAction,
                color = AuraDockColors.OnBackground,
                modifier = Modifier.clickable(role = Role.Button, onClick = onTryAgain).padding(vertical = AuraDockSpacing.sm, horizontal = AuraDockSpacing.xs)
            )
        }
    }
}
