package com.auradock.ui.components

import com.auradock.ui.theme.AuraDockMotion
import com.auradock.ui.theme.AuraDockSizes
import com.auradock.ui.theme.AuraDockRadii
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.auradock.ui.preview.PreviewComponent
import androidx.compose.ui.unit.dp
import com.auradock.ui.theme.AuraDockColors
import com.auradock.ui.theme.AuraDockTheme
import com.auradock.ui.theme.AuraDockTypography
import com.auradock.ui.theme.LocalAuraDockSpacing

@Composable
fun QuickActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isActive: Boolean = false,
    contentColor: Color = AuraDockColors.OnSurfaceVariant
) {
    val spacing = LocalAuraDockSpacing.current
    val shape = RoundedCornerShape(AuraDockRadii.pill)
    val background = if (isActive) AuraDockColors.AccentBlue else AuraDockColors.Surface
    val interactionSource = remember { MutableInteractionSource() }
    val resolvedContentColor = if (isActive) AuraDockColors.OnBackground else contentColor
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(durationMillis = AuraDockMotion.Press),
        label = "buttonScale"
    )
    val shadowElevation by animateFloatAsState(
        targetValue = if (isPressed) 2f else if (isActive) 8f else 4f,
        animationSpec = tween(durationMillis = AuraDockMotion.PressRelease),
        label = "buttonElevation"
    )

    Surface(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .height(AuraDockSizes.button)
            .defaultMinSize(minHeight = AuraDockSizes.button)
            .clickable(
                interactionSource = interactionSource,
                indication = rememberRipple(bounded = true),
                onClick = onClick
            ),
        shape = shape,
        color = background,
        contentColor = resolvedContentColor,
        tonalElevation = AuraDockSizes.none,
        shadowElevation = shadowElevation.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(AuraDockSizes.iconSmall)
                )
                Spacer(modifier = Modifier.width(spacing.sm))
            }
            Text(
                text = text,
                style = AuraDockTypography.WidgetLabel,
                color = resolvedContentColor,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@PreviewComponent
@Composable
private fun QuickActionButtonPreview() {
    AuraDockTheme {
        QuickActionButton(
            text = "Add Reminder",
            onClick = {},
            isActive = false
        )
    }
}
