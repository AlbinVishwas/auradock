package com.auradock.ui.components

import com.auradock.ui.theme.AuraDockSizes
import com.auradock.ui.theme.AuraDockRadii
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import com.auradock.ui.preview.PreviewComponent
import androidx.compose.ui.unit.dp
import com.auradock.ui.theme.AuraDockColors
import com.auradock.ui.theme.AuraDockTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun StandbyPreviewCard(
    selected: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(AuraDockRadii.widget)
    val selectionModifier = if (selected) {
        Modifier
            .shadow(
                elevation = AuraDockSizes.elevationCard,
                shape = shape,
                ambientColor = AuraDockColors.AccentBlue,
                spotColor = AuraDockColors.AccentBlue
            )
            .border(width = AuraDockSizes.line, color = AuraDockColors.AccentBlue, shape = shape)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .aspectRatio(16f / 9f)
            .then(selectionModifier)
    ) {
        GlassCard(modifier = Modifier.fillMaxSize(), content = content)
    }
}

/**
 * Lays [content] out at [designWidth] (keeping the available aspect ratio) and scales the result
 * down to fit, so a preview looks like the real full-size screen instead of overflowing.
 * Content is centered and scaled about its center, so the scaled copy exactly fills the space.
 */
@Composable
fun ScaledPreview(
    designWidth: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    BoxWithConstraints(modifier = modifier) {
        val scale = maxWidth / designWidth
        val designHeight = designWidth * (maxHeight / maxWidth)
        Box(
            modifier = Modifier
                .requiredSize(designWidth, designHeight)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
        ) {
            content()
        }
    }
}

@PreviewComponent
@Composable
private fun StandbyPreviewCardPreview() {
    AuraDockTheme {
        StandbyPreviewCard(selected = true) {
            Box(modifier = Modifier.fillMaxSize())
        }
    }
}
