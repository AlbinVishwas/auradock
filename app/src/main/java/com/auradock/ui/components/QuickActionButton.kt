package com.auradock.ui.components

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
import androidx.compose.ui.tooling.preview.Preview
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
    val shape = RoundedCornerShape(999.dp)
    val background = if (isActive) AuraDockColors.AccentBlue else AuraDockColors.Surface
    val interactionSource = remember { MutableInteractionSource() }
    val resolvedContentColor = if (isActive) AuraDockColors.OnBackground else contentColor
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(durationMillis = 110),
        label = "buttonScale"
    )
    val shadowElevation by animateFloatAsState(
        targetValue = if (isPressed) 2f else if (isActive) 8f else 4f,
        animationSpec = tween(durationMillis = 180),
        label = "buttonElevation"
    )

    Surface(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .height(48.dp)
            .defaultMinSize(minHeight = 48.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = rememberRipple(bounded = true),
                onClick = onClick
            ),
        shape = shape,
        color = background,
        contentColor = resolvedContentColor,
        tonalElevation = 0.dp,
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
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(spacing.sm))
            }
            Text(
                text = text,
                style = AuraDockTypography.WidgetLabel,
                color = resolvedContentColor
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
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
