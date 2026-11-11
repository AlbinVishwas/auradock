package com.auradock.ui.onboarding

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.auradock.ui.components.AuraSheet
import com.auradock.ui.components.GreyTextButton
import com.auradock.ui.theme.AuraDockColors
import com.auradock.ui.theme.AuraDockSpacing
import com.auradock.ui.theme.AuraDockTypography

/**
 * The permission card as a sheet, shown at the moment a feature first needs it. "Allow" goes on to Android's own prompt or settings
 * page; "Skip" leaves the feature as it is (it can be allowed later, from the feature or from Setup). Put it last in a Box that fills the screen.
 */
@Composable
fun PermissionSheet(permission: SetupPermission, onAllow: () -> Unit, onSkip: () -> Unit, onOpenPrivacy: () -> Unit = {}) {
    AuraSheet(onDismiss = onSkip) {
        Text(
            text = permission.sheetTitle,
            style = AuraDockTypography.SheetTitle,
            color = AuraDockColors.OnBackground,
            modifier = Modifier.padding(top = AuraDockSpacing.xs, bottom = AuraDockSpacing.xs)
        )
        PermissionCard(permission = permission, granted = false, onAllow = onAllow, onSkip = onSkip, containerColor = AuraDockColors.Surface)
        GreyTextButton(text = "Privacy policy", onClick = onOpenPrivacy, modifier = Modifier.fillMaxWidth())
    }
}
