package com.auradock.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.auradock.ui.theme.AuraDockColors
import com.auradock.ui.theme.AuraDockSpacing
import com.auradock.ui.theme.AuraDockTypography

/** What the Setup page shows. */
data class SetupUiState(
    /** The permissions that are currently allowed. */
    val granted: Set<SetupPermission>,
    val screensaverSelected: Boolean
)

/** What the user can do on the Setup page. */
data class SetupActions(
    val onAllowPermission: (SetupPermission) -> Unit = {},
    val onOpenScreensaverSettings: () -> Unit = {},
    val onOpenDisplaySettings: () -> Unit = {}
)

/** How docking works, and every permission with its current status, so ones skipped in the walkthrough can be allowed later. */
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
            .padding(horizontal = AuraDockSpacing.mdl, vertical = AuraDockSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(AuraDockSpacing.smd)
    ) {
        Text(text = "Setup", color = AuraDockColors.OnBackground, style = AuraDockTypography.ScreenTitle)
        Text(
            text = "Everything AuraDock can use. Each one is optional; the app works without it and just shows less.",
            style = AuraDockTypography.Secondary,
            color = AuraDockColors.OnSurfaceVariant
        )
        DockExplainer(
            screensaverSelected = state.screensaverSelected,
            onChooseScreensaver = actions.onOpenScreensaverSettings,
            onSetScreenTimeout = actions.onOpenDisplaySettings,
            showTitle = false
        )
        SetupPermission.entries.forEach { permission ->
            PermissionCard(
                permission = permission,
                granted = permission in state.granted,
                onAllow = { actions.onAllowPermission(permission) }
            )
        }
    }
}
