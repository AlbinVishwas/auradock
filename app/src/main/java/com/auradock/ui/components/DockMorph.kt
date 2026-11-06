package com.auradock.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import com.auradock.ui.theme.AuraDockColors
import com.auradock.ui.theme.AuraDockMotion
import com.auradock.ui.theme.AuraDockTypography

/** Starts the Dock now morph from [Rect] (the pill, in window coordinates); runs the callback once the pill has filled the screen. */
typealias DockMorphStarter = (from: Rect, onFilled: () -> Unit) -> Unit

/** Screens read this to start the morph. Without a host (previews) the callback runs at once. */
val LocalDockMorph = staticCompositionLocalOf<DockMorphStarter> { { _: Rect, onFilled: () -> Unit -> onFilled() } }

/**
 * Container transform, first half: the pill grows from where it sits into the whole screen (450 ms, emphasized), going from
 * cream to black. The dock then scales in from its thumbnail size (see the root transition in MainActivity), so the
 * thumbnail's clock grows into the dock's clock.
 */
@Composable
fun DockMorphHost(content: @Composable () -> Unit) {
    var request by remember { mutableStateOf<Pair<Rect, () -> Unit>?>(null) }
    Box(Modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalDockMorph provides ({ from: Rect, done: () -> Unit -> request = from to done } as DockMorphStarter)) { content() }
        request?.let { (from, done) -> MorphOverlay(from) { request = null; done() } }
    }
}

@Composable
private fun MorphOverlay(from: Rect, onFilled: () -> Unit) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, AuraDockMotion.tween(AuraDockMotion.DockMorph, AuraDockMotion.EasingEmphasized))
        onFilled()
    }
    val p = progress.value
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val fullW = with(density) { maxWidth.toPx() }
        val fullH = with(density) { maxHeight.toPx() }
        val left = from.left * (1 - p)
        val top = from.top * (1 - p)
        val w = from.width + (fullW - from.width) * p
        val h = from.height + (fullH - from.height) * p
        val radius = (from.height / 2f) * (1 - p)
        Box(
            modifier = Modifier
                .offset { IntOffset(left.toInt(), top.toInt()) }
                .size(with(density) { w.toDp() }, with(density) { h.toDp() })
                .clip(RoundedCornerShape(with(density) { radius.toDp() }))
                .background(lerp(AuraDockColors.Accent, AuraDockColors.Background, p)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Dock now",
                style = AuraDockTypography.Button,
                color = AuraDockColors.OnAccent,
                modifier = Modifier.alpha((1f - p * 2.5f).coerceIn(0f, 1f))
            )
        }
    }
}

/** Reports where a view sits in the window, for [DockMorphStarter]. */
fun Modifier.reportBounds(onBounds: (Rect) -> Unit): Modifier = onGloballyPositioned { onBounds(it.boundsInRoot()) }
