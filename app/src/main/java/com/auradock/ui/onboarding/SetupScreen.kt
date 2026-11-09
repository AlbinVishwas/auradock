package com.auradock.ui.onboarding

import com.auradock.ui.theme.fromScreenBottom
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import com.auradock.screensaver.ScreensaverTestUiState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import com.auradock.ui.components.BackArrow
import com.auradock.ui.theme.AuraDockColors
import com.auradock.ui.theme.AuraDockRadii
import com.auradock.ui.theme.AuraDockSizes
import com.auradock.ui.theme.AuraDockSpacing
import com.auradock.ui.theme.AuraDockTypography
import com.auradock.ui.theme.fromScreenTop

/** Taps on the version label that open the hidden debug page. */
const val DEBUG_TAPS = 7

/** What the Setup page shows. */
data class SetupUiState(
    /** The permissions that are currently allowed. */
    val granted: Set<SetupPermission>,
    val screensaverSelected: Boolean,
    val screensaverTest: ScreensaverTestUiState = ScreensaverTestUiState(),
    /** E.g. "0.2.2 (8)". Tapping it seven times opens the hidden debug page. */
    val version: String = ""
)

/** What the user can do on the Setup page. */
data class SetupActions(
    val onAllowPermission: (SetupPermission) -> Unit = {},
    val onOpenScreensaverSettings: () -> Unit = {},
    val onOpenDisplaySettings: () -> Unit = {},
    val onOpenScreensaverTest: () -> Unit = {},
    val onOpenDebug: () -> Unit = {}
)

/** Every permission with its status (handoff Setup), so ones skipped in the walkthrough can be allowed later. */
@Composable
fun SetupScreen(
    state: SetupUiState,
    actions: SetupActions,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AuraDockColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(start = AuraDockSpacing.mdl, end = AuraDockSpacing.mdl, top = fromScreenTop(AuraDockSpacing.xxl), bottom = fromScreenBottom(AuraDockSpacing.xxl - AuraDockSpacing.sm)),
        verticalArrangement = Arrangement.spacedBy(AuraDockSpacing.sm)
    ) {
        BackArrow(Modifier.offset(x = -AuraDockSpacing.sm))
        Column(modifier = Modifier.padding(start = AuraDockSpacing.sm, end = AuraDockSpacing.sm, bottom = AuraDockSpacing.md)) {
            Text(text = "Setup and permissions", color = AuraDockColors.OnBackground, style = AuraDockTypography.StepTitle)
            Text(
                text = "${state.granted.size} of ${SetupPermission.entries.size} allowed",
                style = AuraDockTypography.Secondary,
                color = AuraDockColors.OnSurfaceVariant,
                modifier = Modifier.padding(top = AuraDockSpacing.smd - AuraDockSpacing.xxs)
            )
        }
        SetupPermission.entries.forEach { permission ->
            PermissionCard(
                permission = permission,
                granted = permission in state.granted,
                onAllow = { actions.onAllowPermission(permission) },
                showStatus = true
            )
        }
        // Not in the handoff: the way back to the screensaver and screen-timeout settings the walkthrough pointed to.
        val shape = RoundedCornerShape(AuraDockRadii.row)
        Row(
            modifier = Modifier
                .padding(top = AuraDockSpacing.sm)
                .fillMaxWidth()
                .clip(shape)
                .border(AuraDockSizes.line, AuraDockColors.TileOutline, shape)
                .clickable(role = Role.Button, onClick = actions.onOpenScreensaverTest)
                .padding(horizontal = AuraDockSpacing.md + AuraDockSpacing.xxs, vertical = AuraDockSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Screensaver test", style = AuraDockTypography.RowLabel, color = AuraDockColors.OnBackground, modifier = Modifier.weight(1f))
            Text(
                text = when {
                    state.screensaverTest.appeared -> "✓ Appeared on its own"
                    state.screensaverTest.selected -> "Waiting for it"
                    else -> "Not set up"
                },
                style = AuraDockTypography.Meta,
                color = AuraDockColors.OnSurfaceVariant
            )
        }
        if (state.version.isNotEmpty()) {
            var taps by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(0) }
            Text(
                text = "AuraDock ${state.version}",
                style = AuraDockTypography.Meta,
                color = AuraDockColors.Tertiary,
                modifier = Modifier
                    .padding(top = AuraDockSpacing.md)
                    .fillMaxWidth()
                    .clickable(interactionSource = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) {
                        taps++
                        if (taps >= DEBUG_TAPS) {
                            taps = 0
                            actions.onOpenDebug()
                        }
                    }
                    .padding(vertical = AuraDockSpacing.sm),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
