package com.auradock.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import com.auradock.ui.theme.AuraDockMotion
import kotlinx.coroutines.launch

/**
 * When a row's place in its column changes (a done task sinking to the bottom, the rows above it closing up), it glides from where it
 * was to where it is now instead of jumping. Put it on each row of a plain Column; it reads the row's place and shifts the drawing only.
 */
fun Modifier.animateRowPlacement(): Modifier = composed {
    val shift = remember { Animatable(0f) }
    var lastY by remember { mutableStateOf<Int?>(null) }
    val scope = rememberCoroutineScope()
    Modifier
        .onGloballyPositioned { coords ->
            val y = coords.positionInParent().y.toInt()
            val before = lastY
            lastY = y
            if (before != null && before != y) {
                scope.launch {
                    shift.snapTo(shift.value + (before - y))
                    shift.animateTo(0f, AuraDockMotion.tween(AuraDockMotion.Standard))
                }
            }
        }
        .graphicsLayer { translationY = shift.value }
}
