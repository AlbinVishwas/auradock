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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auradock.ui.theme.AuraDockColors
import com.auradock.ui.theme.AuraDockTypography

/** Every permission with its current status, so ones skipped in the walkthrough can be allowed later. */
@Composable
fun SetupScreen(
    permissionGranted: (SetupPermission) -> Boolean,
    onAllowPermission: (SetupPermission) -> Unit,
    screensaverSelected: Boolean,
    onOpenScreensaverSettings: () -> Unit,
    onOpenDisplaySettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "Setup", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.SemiBold)
        Text(
            text = "Everything AuraDock can use. Each one is optional; the app works without it and just shows less.",
            style = AuraDockTypography.Caption,
            color = AuraDockColors.OnSurfaceVariant
        )
        DockExplainer(
            screensaverSelected = screensaverSelected,
            onChooseScreensaver = onOpenScreensaverSettings,
            onSetScreenTimeout = onOpenDisplaySettings,
            showTitle = false
        )
        SetupPermission.entries.forEach { permission ->
            PermissionCard(
                permission = permission,
                granted = permissionGranted(permission),
                onAllow = { onAllowPermission(permission) }
            )
        }
    }
}
