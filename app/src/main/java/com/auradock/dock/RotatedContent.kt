package com.auradock.dock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.min

/**
 * Shows [content] turned [degrees] clockwise (0, 90, 180 or 270) inside a black window.
 *
 * The dock is always landscape-shaped, so the content is laid out as a "wide" box (long side of the window
 * horizontal) and then turned. If the window is portrait and degrees is 90 or 270, the wide box turned on its
 * side exactly fills the tall window. If the window is already landscape and degrees is 0 or 180, nothing
 * about the shape changes. Touches never reach the content: the dream is not interactive.
 */
@Composable
fun RotatedContent(degrees: Int, content: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black)) {
        val longSide = max(maxWidth, maxHeight)
        val shortSide = min(maxWidth, maxHeight)
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .requiredSize(longSide, shortSide)
                .graphicsLayer { rotationZ = degrees.toFloat() }
        ) {
            content()
        }
    }
}
