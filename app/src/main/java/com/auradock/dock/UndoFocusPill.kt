package com.auradock.dock

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import com.auradock.ui.theme.AuraDockColors
import com.auradock.ui.theme.AuraDockMotion
import com.auradock.ui.theme.AuraDockSizes
import com.auradock.ui.theme.AuraDockSpacing
import com.auradock.ui.theme.AuraDockTypography

/**
 * A small "Undo focus" control that fades in on the dock after a touch and goes by itself ([visible] is driven for a few seconds by the
 * ViewModel). It turns Do Not Disturb back to what it was for this session, without ending the dock. Self-contained: it only needs to
 * know whether to show and what to do when tapped.
 */
@Composable
fun UndoFocusPill(visible: Boolean, onUndo: () -> Unit, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(AuraDockMotion.Fast)),
        exit = fadeOut(tween(AuraDockMotion.Slow)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .height(AuraDockSizes.smallPill)
                .clip(RoundedCornerShape(AuraDockSizes.smallPill / 2))
                .background(AuraDockColors.QuietPill)
                .clickable(role = Role.Button, onClick = onUndo)
                .padding(horizontal = AuraDockSpacing.md),
            contentAlignment = Alignment.Center
        ) { Text(text = "Undo focus", style = AuraDockTypography.SmallAction, color = AuraDockColors.OnBackground) }
    }
}
