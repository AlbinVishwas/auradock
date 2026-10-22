package com.auradock.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.auradock.ui.theme.AuraDockColors
import com.auradock.ui.theme.AuraDockTheme

@Composable
fun StandbyPreviewCard(
    selected: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(28.dp)
    val selectionModifier = if (selected) {
        Modifier
            .shadow(
                elevation = 16.dp,
                shape = shape,
                ambientColor = AuraDockColors.AccentBlue,
                spotColor = AuraDockColors.AccentBlue
            )
            .border(width = 1.dp, color = AuraDockColors.AccentBlue, shape = shape)
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

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun StandbyPreviewCardPreview() {
    AuraDockTheme {
        StandbyPreviewCard(selected = true) {
            Box(modifier = Modifier.fillMaxSize())
        }
    }
}
