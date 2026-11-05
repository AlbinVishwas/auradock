package com.auradock.ui.onboarding

import com.auradock.ui.theme.AuraDockRadii
import com.auradock.ui.theme.AuraDockIllustration
import com.auradock.ui.theme.AuraDockSpacing
import com.auradock.ui.theme.AuraDockMotion
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.auradock.ui.components.QuickActionButton
import com.auradock.ui.theme.AuraDockColors
import com.auradock.ui.theme.AuraDockTypography
import kotlinx.coroutines.delay

private val CAPTIONS = listOf("Place it sideways on a charger", "The screen times out", "AuraDock appears")

/**
 * What docking looks like, in three looping frames: the phone is put sideways on a stand, the screen dims while a
 * small timer ring runs, then the dock appears.
 */
@Composable
fun DockAnimation(modifier: Modifier = Modifier) {
    var frame by remember { mutableIntStateOf(0) }
    val ring = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            ring.snapTo(0f)
            if (frame == 1) ring.animateTo(1f, tween(AuraDockMotion.IllustrationFrame, easing = LinearEasing))
            else delay(AuraDockMotion.IllustrationFrame.toLong())
            frame = (frame + 1) % 3
        }
    }
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(AuraDockSpacing.sm)) {
        Box(Modifier.fillMaxWidth().height(AuraDockIllustration.height), contentAlignment = Alignment.Center) {
            Crossfade(targetState = frame, animationSpec = tween(AuraDockMotion.Slow), label = "dockFrame") { shown ->
                Box(Modifier.fillMaxWidth().height(AuraDockIllustration.height), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxWidth().height(AuraDockIllustration.height)) { drawFrame(shown, ring.value) }
                    if (shown == 2) Text("10:42", color = AuraDockColors.OnBackground, style = AuraDockTypography.IllustrationClock)
                }
            }
        }
        Crossfade(targetState = frame, animationSpec = tween(AuraDockMotion.Slow), label = "dockCaption") {
            Text(CAPTIONS[it], style = AuraDockTypography.Caption, color = AuraDockColors.OnSurfaceVariant)
        }
    }
}

private fun DrawScope.drawFrame(frame: Int, ringProgress: Float) {
    val phoneW = AuraDockIllustration.phoneWidth.toPx()
    val phoneH = AuraDockIllustration.phoneHeight.toPx()
    val left = (size.width - phoneW) / 2
    val top = AuraDockIllustration.top.toPx()
    val radius = CornerRadius(AuraDockIllustration.phoneRadius.toPx())

    // The stand: a small foot and a lip the phone rests on.
    val standY = top + phoneH + AuraDockIllustration.standGap.toPx()
    drawRoundRect(AuraDockColors.IllustrationStand, Offset(left + phoneW * 0.25f, standY), Size(phoneW * 0.5f, AuraDockIllustration.standHeight.toPx()), CornerRadius(AuraDockIllustration.standCorner.toPx()))
    drawRoundRect(AuraDockColors.IllustrationStand, Offset(left + phoneW * 0.35f, standY - AuraDockIllustration.standHeight.toPx()), Size(phoneW * 0.3f, AuraDockIllustration.standHeight.toPx()))

    // The phone, sideways.
    drawRoundRect(AuraDockColors.IllustrationPhone, Offset(left, top), Size(phoneW, phoneH), radius)
    val screenTopLeft = Offset(left + AuraDockIllustration.bezel.toPx(), top + AuraDockIllustration.bezel.toPx())
    val screenSize = Size(phoneW - 2 * AuraDockIllustration.bezel.toPx(), phoneH - 2 * AuraDockIllustration.bezel.toPx())
    val screenRadius = CornerRadius(AuraDockIllustration.screenRadius.toPx())
    when (frame) {
        0 -> { // an ordinary app, bright
            drawRoundRect(AuraDockColors.IllustrationApp, screenTopLeft, screenSize, screenRadius)
            for (i in 0..2) {
                drawRoundRect(AuraDockColors.IllustrationAppLine, Offset(screenTopLeft.x + AuraDockIllustration.appLineInset.toPx(), screenTopLeft.y + (AuraDockIllustration.appLineInset + AuraDockIllustration.appLineStep * i).toPx()), Size((AuraDockIllustration.appLineWidth - AuraDockIllustration.appLineShrink * i).toPx(), AuraDockIllustration.appLineHeight.toPx()), CornerRadius(AuraDockIllustration.standCorner.toPx()))
            }
        }
        1 -> { // dimming, with the timer ring
            drawRoundRect(AuraDockColors.IllustrationDimScreen, screenTopLeft, screenSize, screenRadius)
            val ringSize = AuraDockIllustration.ring.toPx()
            val ringTopLeft = Offset(size.width / 2 - ringSize / 2, top + phoneH / 2 - ringSize / 2)
            drawArc(AuraDockColors.IllustrationRingTrack, 0f, 360f, false, ringTopLeft, Size(ringSize, ringSize), style = Stroke(AuraDockIllustration.ringStroke.toPx()))
            drawArc(AuraDockColors.Coral, -90f, 360f * ringProgress, false, ringTopLeft, Size(ringSize, ringSize), style = Stroke(AuraDockIllustration.ringStroke.toPx(), cap = StrokeCap.Round))
        }
        else -> drawRoundRect(AuraDockColors.Background, screenTopLeft, screenSize, screenRadius) // the dock; the clock is drawn on top
    }
}

/**
 * The docking explanation with its two settings steps, used by the onboarding step and the Setup page.
 * [screensaverSelected] is true when Android reports AuraDock as the chosen screensaver; it adds a checkmark.
 */
@Composable
fun DockExplainer(
    screensaverSelected: Boolean,
    onChooseScreensaver: () -> Unit,
    onSetScreenTimeout: () -> Unit,
    modifier: Modifier = Modifier,
    showTitle: Boolean = true
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(AuraDockSpacing.md)) {
        if (showTitle) {
            Text("Dock it, and AuraDock appears", color = AuraDockColors.OnBackground, style = AuraDockTypography.ScreenTitle)
        }
        Text(
            text = "Place your phone sideways on a charger. When the screen times out, AuraDock takes over automatically. " +
                "Upright or lying flat? It stays out of the way.",
            style = AuraDockTypography.Body,
            color = AuraDockColors.OnSurfaceVariant
        )
        DockAnimation()
        QuickActionButton(
            text = if (screensaverSelected) "✓ AuraDock is your screensaver" else "Choose AuraDock as screensaver",
            onClick = onChooseScreensaver,
            isActive = !screensaverSelected
        )
        Text("Select AuraDock in the list.", style = AuraDockTypography.Caption, color = AuraDockColors.OnSurfaceVariant)
        QuickActionButton(text = "Set screen timeout", onClick = onSetScreenTimeout)
        Text(
            "A shorter timeout, like 30 seconds, makes AuraDock appear faster.",
            style = AuraDockTypography.Caption,
            color = AuraDockColors.OnSurfaceVariant
        )
    }
}
