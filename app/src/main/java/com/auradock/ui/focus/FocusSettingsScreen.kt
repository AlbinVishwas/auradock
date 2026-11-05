package com.auradock.ui.focus

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auradock.ui.components.QuickActionButton
import com.auradock.ui.theme.AuraDockColors
import com.auradock.ui.theme.AuraDockTypography

/** The Focus settings page, opened from the dashboard. */
@Composable
fun FocusSettingsScreen(
    focusWhileDocked: Boolean,
    hasDndAccess: Boolean,
    onToggleFocus: (Boolean) -> Unit,
    onOpenDndSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(text = "Focus", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.SemiBold)

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
                Switch(checked = focusWhileDocked, onCheckedChange = onToggleFocus)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "While the dock is showing, Do Not Disturb silences everything except alarms and calls from your " +
                    "starred contacts. When the dock closes, your own Do Not Disturb settings are put back exactly as " +
                    "they were. If Do Not Disturb is already on, AuraDock leaves it alone.",
                style = AuraDockTypography.Caption,
                color = AuraDockColors.OnSurfaceVariant
            )
            if (!hasDndAccess) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "AuraDock needs Do Not Disturb access to do this (newer Android calls it \"Modes access\").",
                    style = AuraDockTypography.WidgetLabel,
                    color = Color(0xFFFFB300)
                )
                Spacer(Modifier.height(8.dp))
                QuickActionButton(text = "Allow Do Not Disturb access", onClick = onOpenDndSettings, isActive = true)
            } else if (focusWhileDocked) {
                Spacer(Modifier.height(12.dp))
                Text(text = "Ready: Do Not Disturb turns on when you dock.", style = AuraDockTypography.Caption, color = AuraDockColors.OnSurfaceVariant)
            }
        }
    }
}

@Composable
internal fun SettingsCard(content: @Composable () -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), color = AuraDockColors.Surface, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) { content() }
    }
}
