package com.auradock.ui.components

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import com.auradock.ui.theme.AuraDockColors
import com.auradock.ui.theme.AuraDockSizes

/** The back arrow at the top left of a settings screen: a 48 dp tap area around a 24 dp arrow. [onBack] defaults to the system back. */
@Composable
fun BackArrow(modifier: Modifier = Modifier, onBack: (() -> Unit)? = null) {
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    Box(
        modifier = modifier.size(AuraDockSizes.touchTarget).clickable(role = Role.Button) { if (onBack != null) onBack() else dispatcher?.onBackPressed() },
        contentAlignment = Alignment.Center
    ) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = AuraDockColors.OnBackground, modifier = Modifier.size(AuraDockSizes.icon)) }
}
