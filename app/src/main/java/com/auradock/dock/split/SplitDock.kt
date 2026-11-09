package com.auradock.dock.split

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.auradock.dock.DockActions
import com.auradock.dock.DockUiState
import com.auradock.dock.BoldStripLine
import com.auradock.dock.boldTimeString
import com.auradock.dock.dockTouch
import com.auradock.dock.world.dockIsNight
import com.auradock.ui.theme.AuraDockColors
import com.auradock.ui.theme.AuraDockMotion
import com.auradock.ui.theme.AuraDockSplit
import com.auradock.ui.theme.AuraDockSplitTones
import com.auradock.ui.theme.AuraDockTypography
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.min

/** Where one half's stack is: the widget showing, how far a swipe has got (-1..1), and whether the dots are showing. */
@Stable
class StackState {
    var index by mutableIntStateOf(0)
    val progress = Animatable(0f)
    var indicatorKick by mutableIntStateOf(0)
    var indicatorOn by mutableStateOf(false)
}

/**
 * The Split dock: two independent stacks side by side, each 405 x 364 in the frames' 915 x 412. Swiping up or down on a half changes only that
 * half (the old widget slides and fades out, the new one slides in, loses its blur and fades in, and a column of dots shows where you are).
 * One tap shows "Undo focus", two leave, a long press edits that half's stack. At least one half always shows the time; if neither does the
 * small corner time shows. By night the tones step down and the dial loses its second hand.
 */
@Composable
fun SplitDock(
    state: DockUiState,
    use24Hour: Boolean,
    strip: List<String>,
    caption: String?,
    actions: DockActions,
    modifier: Modifier = Modifier
) {
    val stacks = state.split.stacks
    val zone = remember { ZoneId.systemDefault() }
    val night = dockIsNight(state.timeMillis, state.world.home, zone)
    val env = WidgetEnv(state, use24Hour, AuraDockSplitTones.of(night))
    val leftKinds = visibleStack(stacks.left, nowPlaying = true)
    val rightKinds = visibleStack(stacks.right, nowPlaying = true)
    val left = remember { StackState() }
    val right = remember { StackState() }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val pitchPx = with(density) { AuraDockSplit.Pitch.toPx() }
    val stripShown = strip.isNotEmpty() || caption != null

    // Keep the shown index inside the stack when widgets are removed.
    if (left.index >= leftKinds.size) left.index = 0
    if (right.index >= rightKinds.size) right.index = 0

    BoxWithConstraints(modifier.fillMaxSize()) {
        val screenW = maxWidth
        val halfW = (screenW - AuraDockSplit.SideInset * 2 - AuraDockSplit.Gap) / 2
        val halfH = maxHeight - AuraDockSplit.TopInset * 2
        val fit = min(min(halfW / AuraDockSplit.HalfWidth, halfH / AuraDockSplit.HalfHeight), 1f)

        fun settle(side: StackState, size: Int) {
            scope.launch {
                val p = side.progress.value
                val target = when {
                    size <= 1 -> 0f
                    p > AuraDockSplit.CommitShare -> 1f
                    p < -AuraDockSplit.CommitShare -> -1f
                    else -> 0f
                }
                side.progress.animateTo(target, tween(AuraDockMotion.Standard, easing = FastOutSlowInEasing))
                if (target != 0f) side.index = cycleIndex(side.index, size, forward = target > 0f)
                side.progress.snapTo(0f)
                side.indicatorKick++
            }
        }

        var dragging: StackState? = null
        var draggingSize = 0
        val gestures = Modifier
            .dockTouch(actions.touch)
            .pointerInput(leftKinds.size, rightKinds.size) {
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        val isLeft = offset.x < size.width / 2f
                        dragging = if (isLeft) left else right
                        draggingSize = if (isLeft) leftKinds.size else rightKinds.size
                        dragging?.let { s -> scope.launch { s.progress.stop() }; s.indicatorKick++ }
                    },
                    onVerticalDrag = { change, dy ->
                        val s = dragging
                        if (s != null && draggingSize > 1) {
                            change.consume()
                            scope.launch { s.progress.snapTo((s.progress.value - dy / pitchPx).coerceIn(-1f, 1f)) }
                        }
                    },
                    onDragEnd = { dragging?.let { settle(it, draggingSize) }; dragging = null },
                    onDragCancel = { dragging?.let { settle(it, draggingSize) }; dragging = null }
                )
            }

        Box(Modifier.fillMaxSize().then(gestures)) {
            Box(
                Modifier.fillMaxSize().graphicsLayer {
                    if (stripShown) {
                        val s = AuraDockSplit.NotificationScale
                        scaleX = s; scaleY = s
                        translationY = -AuraDockSplit.NotificationRise.toPx()
                        transformOrigin = TransformOrigin(0.5f, 0f)
                    }
                }
            ) {
                StackHalf(leftKinds, left, env, halfW, halfH, fit, alignEnd = false, screenW = screenW, pitch = AuraDockSplit.Pitch)
                StackHalf(rightKinds, right, env, halfW, halfH, fit, alignEnd = true, screenW = screenW, pitch = AuraDockSplit.Pitch)
            }

            StackDots(leftKinds.size, left, Alignment.CenterStart, env)
            StackDots(rightKinds.size, right, Alignment.CenterEnd, env)

            // Neither half shows the time: a small one in the top margin, so the time is never missing.
            val shownLeft = leftKinds.getOrNull(left.index)
            val shownRight = rightKinds.getOrNull(right.index)
            if (needsCornerTime(shownLeft, shownRight)) {
                Text(
                    boldTimeString(state.timeMillis, use24Hour), style = AuraDockTypography.SplitCornerTime, color = env.tones.tertiary,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = AuraDockSplit.CornerTimeTop)
                )
            }

            if (stripShown) {
                Column(
                    Modifier.align(Alignment.BottomCenter).padding(bottom = AuraDockSplit.TopInset),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    strip.forEachIndexed { i, line -> BoldStripLine(line, if (i == strip.lastIndex) caption else null) }
                    if (strip.isEmpty() && caption != null) Text(caption, color = AuraDockColors.HeldCaption, style = AuraDockTypography.BoldStrip, maxLines = 1)
                }
            }
        }
    }
}

/** One half: the shown widget, and during a swipe the one arriving. Clipped to its side of the screen, full height, as the frames clip it. */
@Composable
private fun StackHalf(
    kinds: List<WidgetKind>,
    stack: StackState,
    env: WidgetEnv,
    halfW: Dp,
    halfH: Dp,
    fit: Float,
    alignEnd: Boolean,
    screenW: Dp,
    pitch: Dp
) {
    val p = stack.progress.value
    val n = kinds.size
    val shown = kinds[stack.index.coerceIn(0, n - 1)]
    val arriving = when {
        p > 0f -> kinds[cycleIndex(stack.index, n, true)]
        p < 0f -> kinds[cycleIndex(stack.index, n, false)]
        else -> null
    }
    val look = swipeLook(abs(p), AuraDockSplit.OutgoingSlope, AuraDockSplit.IncomingSlope, AuraDockSplit.BlurMax.value)
    val slotX = if (alignEnd) screenW / 2 - AuraDockSplit.SideInset - halfW else AuraDockSplit.SideInset
    Box(
        Modifier
            .offset(x = if (alignEnd) screenW / 2 else 0.dp)
            .width(screenW / 2)
            .fillMaxHeight()
            .clipToBounds()
    ) {
        Slot(shown, env, slotX, AuraDockSplit.TopInset - pitch * p, look.outgoingAlpha, 0.dp, halfW, halfH, fit)
        if (arriving != null) {
            val sign = if (p > 0f) 1f else -1f
            Slot(arriving, env, slotX, AuraDockSplit.TopInset + pitch * ((1f - abs(p)) * sign), look.incomingAlpha, look.incomingBlurDp.dp, halfW, halfH, fit)
        }
    }
}

@Composable
private fun Slot(kind: WidgetKind, env: WidgetEnv, x: Dp, y: Dp, alpha: Float, blur: Dp, halfW: Dp, halfH: Dp, fit: Float) {
    Box(
        Modifier
            .offset(x = x, y = y)
            .size(halfW, halfH)
            .alpha(alpha)
            .then(if (blur > 0.dp) Modifier.blur(blur) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.requiredSize(AuraDockSplit.HalfWidth, AuraDockSplit.HalfHeight).graphicsLayer { scaleX = fit; scaleY = fit }) {
            SplitWidget(kind, env)
        }
    }
}

/** The column of dots at the screen edge: the current widget white, the rest dim. Shows while swiping and briefly after. */
@Composable
private fun androidx.compose.foundation.layout.BoxScope.StackDots(count: Int, stack: StackState, side: Alignment, env: WidgetEnv) {
    LaunchedEffect(stack.indicatorKick) {
        if (stack.indicatorKick > 0) {
            stack.indicatorOn = true
            delay(AuraDockSplit.IndicatorHoldMillis.toLong())
            stack.indicatorOn = false
        }
    }
    val dragging = stack.progress.value != 0f
    val visible by animateFloatAsState(if (count > 1 && (stack.indicatorOn || dragging)) 1f else 0f, tween(AuraDockMotion.Standard), label = "stackDots")
    if (visible <= 0f) return
    val current = if (stack.progress.value > 0f) cycleIndex(stack.index, count, true) else if (stack.progress.value < 0f) cycleIndex(stack.index, count, false) else stack.index
    Column(
        Modifier.align(side).padding(horizontal = AuraDockSplit.IndicatorInset).alpha(visible),
        verticalArrangement = Arrangement.spacedBy(AuraDockSplit.IndicatorGap)
    ) {
        repeat(count) { i ->
            Box(Modifier.size(AuraDockSplit.IndicatorDot).clip(CircleShape).background(if (i == current) AuraDockColors.DockText else AuraDockColors.Inactive))
        }
    }
}
