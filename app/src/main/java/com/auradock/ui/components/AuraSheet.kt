package com.auradock.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import com.auradock.ui.theme.AuraDockColors
import com.auradock.ui.theme.AuraDockMotion
import com.auradock.ui.theme.AuraDockRadii
import com.auradock.ui.theme.AuraDockSizes
import com.auradock.ui.theme.AuraDockSpacing
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * The bottom sheet of the handoff: a dark sheet with 28 dp top corners and a grab handle, over a 60% black scrim. It rises on the
 * style guide's spring (damping 0.9, stiffness 380) and leaves the same way; drag it down or tap the scrim to close it. Material's
 * own sheet cannot take a custom spring, so this one is drawn here. Put it last in a Box that fills the screen: it covers what is under it.
 */
@Composable
fun AuraSheet(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    run {
        val scope = rememberCoroutineScope()
        // The sheet starts one screen height below and rises; closing sends it back.
        val screenHeight = LocalView.current.height.toFloat()
        val offset = remember { Animatable(screenHeight) }
        val scrim = remember { Animatable(0f) }
        var height by remember { mutableIntStateOf(0) }

        fun close() {
            scope.launch { scrim.animateTo(0f, tween(AuraDockMotion.Slow)) }
            scope.launch {
                offset.animateTo(screenHeight, AuraDockMotion.softSpring())
                onDismiss()
            }
        }
        LaunchedEffect(Unit) {
            launch { scrim.animateTo(AuraDockMotion.ScrimAlpha, tween(AuraDockMotion.Slow)) }
            offset.animateTo(0f, AuraDockMotion.softSpring())
        }
        BackHandler { close() }

        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = if (AuraDockMotion.ScrimAlpha == 0f) 0f else scrim.value / AuraDockMotion.ScrimAlpha }
                    .background(AuraDockColors.Scrim)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = ::close)
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .imePadding()
                    .offset { IntOffset(0, offset.value.roundToInt()) }
                    .onSizeChanged { height = it.height }
                    .draggable(
                        orientation = Orientation.Vertical,
                        state = rememberDraggableState { delta -> scope.launch { offset.snapTo((offset.value + delta).coerceAtLeast(0f)) } },
                        onDragStopped = { velocity ->
                            if (offset.value > height * 0.3f || velocity > 1500f) close()
                            else scope.launch { offset.animateTo(0f, AuraDockMotion.softSpring()) }
                        }
                    )
                    .clip(RoundedCornerShape(topStart = AuraDockRadii.sheet, topEnd = AuraDockRadii.sheet))
                    .background(AuraDockColors.SurfaceRaised)
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(start = AuraDockSpacing.lg, end = AuraDockSpacing.lg, top = AuraDockSpacing.smd, bottom = AuraDockSpacing.xl + AuraDockSpacing.sm),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(AuraDockSpacing.mdl),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(Modifier.size(AuraDockSizes.handleWidth, AuraDockSizes.handleHeight).clip(RoundedCornerShape(AuraDockSizes.lineThick)).background(AuraDockColors.SheetHandle))
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(AuraDockSpacing.mdl)) { content() }
            }
        }
    }
}
