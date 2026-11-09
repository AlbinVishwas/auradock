package com.auradock.dock.lines

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import com.auradock.ui.theme.AuraDockRadii
import com.auradock.ui.theme.AuraDockSizes
import com.auradock.ui.theme.AuraDockSpacing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.auradock.intention.INTENTION_MAX_CHARS
import com.auradock.intention.cleanIntention
import com.auradock.ui.components.GreyTextButton
import com.auradock.ui.components.AuraTextField
import com.auradock.ui.theme.AuraDockColors
import com.auradock.ui.theme.AuraDockDockLayout
import com.auradock.ui.theme.AuraDockTypography

/**
 * The calm question on the first dock of the day: "What's the one thing today?", a single text field, Save and Skip. It replaces
 * the clock until it is answered. [prefill] is last evening's "tomorrow's top task", when there is one.
 */
@Composable
fun IntentionPrompt(prefill: String, onSave: (String) -> Unit, onSkip: () -> Unit, modifier: Modifier = Modifier) {
    var text by remember { mutableStateOf(prefill) }
    val canSave = cleanIntention(text) != null
    Box(modifier = modifier.fillMaxSize().imePadding(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.width(AuraDockDockLayout.promptWidth),
            verticalArrangement = Arrangement.spacedBy(AuraDockDockLayout.promptGap)
        ) {
            Text(text = "What's the one thing today?", color = AuraDockColors.DockText, style = AuraDockTypography.DockPrompt)
            // One row, so the keyboard (which fills the lower half in landscape) never hides Save.
            Row(horizontalArrangement = Arrangement.spacedBy(AuraDockDockLayout.lineGap), verticalAlignment = Alignment.CenterVertically) {
                AuraTextField(
                    value = text,
                    onValueChange = { if (it.length <= INTENTION_MAX_CHARS) text = it },
                    hint = "One thing",
                    onDone = { if (canSave) onSave(text) },
                    modifier = Modifier.weight(1f)
                )
                SavePill(onClick = { onSave(text) }, enabled = canSave)
                GreyTextButton(text = "Skip", onClick = onSkip)
            }
        }
    }
}

/** The one filled action of the prompt: a cream pill, grey while there is nothing to save. */
@Composable
private fun SavePill(onClick: () -> Unit, enabled: Boolean) {
    Box(
        modifier = Modifier
            .height(AuraDockSizes.field)
            .clip(RoundedCornerShape(AuraDockRadii.pill))
            .background(if (enabled) AuraDockColors.Accent else AuraDockColors.Inactive)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = AuraDockSpacing.lg),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "Save", style = AuraDockTypography.SmallAction, color = if (enabled) AuraDockColors.OnAccent else AuraDockColors.Tertiary)
    }
}
