package com.auradock.ui.onboarding

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auradock.ui.components.QuickActionButton
import com.auradock.ui.theme.AuraDockColors
import com.auradock.ui.theme.AuraDockTypography
import kotlinx.coroutines.delay

private val Coral = Color(0xFFFF6B5E)
private const val FRAME_MS = 2200L

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
            if (frame == 1) ring.animateTo(1f, tween(FRAME_MS.toInt(), easing = LinearEasing))
            else delay(FRAME_MS)
            frame = (frame + 1) % 3
        }
    }
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
            Crossfade(targetState = frame, animationSpec = tween(400), label = "dockFrame") { shown ->
                Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxWidth().height(150.dp)) { drawFrame(shown, ring.value) }
                    if (shown == 2) Text("10:42", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Light)
                }
            }
        }
        Crossfade(targetState = frame, animationSpec = tween(400), label = "dockCaption") {
            Text(CAPTIONS[it], style = AuraDockTypography.Caption, color = AuraDockColors.OnSurfaceVariant)
        }
    }
}

private fun DrawScope.drawFrame(frame: Int, ringProgress: Float) {
    val phoneW = 200.dp.toPx()
    val phoneH = 96.dp.toPx()
    val left = (size.width - phoneW) / 2
    val top = 18.dp.toPx()
    val radius = CornerRadius(14.dp.toPx())

    // The stand: a small foot and a lip the phone rests on.
    val standY = top + phoneH + 4.dp.toPx()
    drawRoundRect(Color(0xFF444444), Offset(left + phoneW * 0.25f, standY), Size(phoneW * 0.5f, 8.dp.toPx()), CornerRadius(4.dp.toPx()))
    drawRoundRect(Color(0xFF444444), Offset(left + phoneW * 0.35f, standY - 8.dp.toPx()), Size(phoneW * 0.3f, 8.dp.toPx()))

    // The phone, sideways.
    drawRoundRect(Color(0xFF2A2A2A), Offset(left, top), Size(phoneW, phoneH), radius)
    val screenTopLeft = Offset(left + 4.dp.toPx(), top + 4.dp.toPx())
    val screenSize = Size(phoneW - 8.dp.toPx(), phoneH - 8.dp.toPx())
    val screenRadius = CornerRadius(10.dp.toPx())
    when (frame) {
        0 -> { // an ordinary app, bright
            drawRoundRect(Color(0xFF3A4A5A), screenTopLeft, screenSize, screenRadius)
            for (i in 0..2) {
                drawRoundRect(Color(0x66FFFFFF), Offset(screenTopLeft.x + 14.dp.toPx(), screenTopLeft.y + (14 + i * 18).dp.toPx()), Size((110 - i * 25).dp.toPx(), 8.dp.toPx()), CornerRadius(4.dp.toPx()))
            }
        }
        1 -> { // dimming, with the timer ring
            drawRoundRect(Color(0xFF151A20), screenTopLeft, screenSize, screenRadius)
            val ringSize = 40.dp.toPx()
            val ringTopLeft = Offset(size.width / 2 - ringSize / 2, top + phoneH / 2 - ringSize / 2)
            drawArc(Color(0x33FFFFFF), 0f, 360f, false, ringTopLeft, Size(ringSize, ringSize), style = Stroke(3.dp.toPx()))
            drawArc(Coral, -90f, 360f * ringProgress, false, ringTopLeft, Size(ringSize, ringSize), style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
        }
        else -> drawRoundRect(Color.Black, screenTopLeft, screenSize, screenRadius) // the dock; the clock is drawn on top
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
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (showTitle) {
            Text("Dock it, and AuraDock appears", color = Color.White, fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.SemiBold)
        }
        Text(
            text = "Place your phone sideways on a charger. When the screen times out, AuraDock takes over automatically. " +
                "Upright or lying flat? It stays out of the way.",
            color = AuraDockColors.OnSurfaceVariant,
            fontSize = 17.sp,
            lineHeight = 24.sp
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
