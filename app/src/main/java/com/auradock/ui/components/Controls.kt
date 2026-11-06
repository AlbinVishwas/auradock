package com.auradock.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.auradock.ui.preview.PreviewComponent
import com.auradock.ui.theme.AuraDockColors
import com.auradock.ui.theme.AuraDockMotion
import com.auradock.ui.theme.AuraDockRadii
import com.auradock.ui.theme.AuraDockSizes
import com.auradock.ui.theme.AuraDockSpacing
import com.auradock.ui.theme.AuraDockTheme
import com.auradock.ui.theme.AuraDockTypography

// The style guide's components (docs/design/STYLE_GUIDE.md): pills, toggle, checkbox, badge, progress dots, blur to focus.

/** The primary pill: 56 dp high, accent fill, black text. One per screen. */
@Composable
fun PrimaryPill(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(AuraDockSizes.pill)
            .clip(RoundedCornerShape(AuraDockRadii.pill))
            .background(if (enabled) AuraDockColors.Accent else AuraDockColors.Inactive)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, style = AuraDockTypography.Button, color = if (enabled) AuraDockColors.OnAccent else AuraDockColors.Tertiary)
    }
}

/** The quiet pill: `#2E2E2E`, for the actions that are not the one main thing on the screen. */
@Composable
fun QuietPill(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = AuraDockSizes.touchTarget)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .height(AuraDockSizes.quietPill)
                .clip(RoundedCornerShape(AuraDockRadii.pill))
                .background(AuraDockColors.QuietPill)
                .padding(horizontal = AuraDockSpacing.md),
            contentAlignment = Alignment.Center
        ) {
            Text(text = text, style = AuraDockTypography.Caption, color = AuraDockColors.OnBackground, maxLines = 1)
        }
    }
}

/** A grey text button: "Skip for now". */
@Composable
fun GreyTextButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = AuraDockSizes.touchTarget)
            .clickable(onClick = onClick)
            .padding(horizontal = AuraDockSpacing.md),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, style = AuraDockTypography.Body, color = AuraDockColors.OnSurfaceVariant, maxLines = 1)
    }
}

/** A toggle: accent track with a black thumb when on; grey track and thumb when off. */
@Composable
fun AuraSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val track by animateColorAsState(if (checked) AuraDockColors.Accent else AuraDockColors.Inactive, label = "track")
    val thumb by animateColorAsState(if (checked) AuraDockColors.OnAccent else AuraDockColors.Tertiary, label = "thumb")
    val pad = (AuraDockSizes.toggleHeight - AuraDockSizes.toggleThumb) / 2
    val x by animateDpAsState(
        if (checked) AuraDockSizes.toggleWidth - AuraDockSizes.toggleThumb - pad else pad,
        animationSpec = AuraDockMotion.softSpring(), label = "thumbX"
    )
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = AuraDockSizes.touchTarget, minWidth = AuraDockSizes.touchTarget)
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onCheckedChange(!checked) }
            )
            .alpha(if (enabled) 1f else 0.4f),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(AuraDockSizes.toggleWidth, AuraDockSizes.toggleHeight)
                .clip(CircleShape)
                .background(track)
        ) {
            Box(
                modifier = Modifier
                    .offset(x = x, y = pad)
                    .size(AuraDockSizes.toggleThumb)
                    .clip(CircleShape)
                    .background(thumb)
            )
        }
    }
}

/**
 * A checkbox: selected (accent fill, black tick), empty (outline), or dimmed (an empty one that cannot be picked because
 * the limit is reached).
 */
@Composable
fun AuraCheckbox(checked: Boolean, modifier: Modifier = Modifier, dimmed: Boolean = false) {
    val shape = RoundedCornerShape(AuraDockRadii.tile)
    val tick = AuraDockColors.OnAccent
    val strokePx = with(androidx.compose.ui.platform.LocalDensity.current) { AuraDockSizes.checkStroke.toPx() }
    Box(
        modifier = modifier
            .size(AuraDockSizes.checkbox)
            .clip(shape)
            .then(
                if (checked) Modifier.background(AuraDockColors.Accent)
                else Modifier.border(AuraDockSizes.lineThick, if (dimmed) AuraDockColors.Inactive else AuraDockColors.Outline, shape)
            )
    ) {
        if (checked) {
            Canvas(Modifier.size(AuraDockSizes.checkbox)) {
                val p = Path().apply {
                    moveTo(size.width * 0.26f, size.height * 0.52f)
                    lineTo(size.width * 0.44f, size.height * 0.70f)
                    lineTo(size.width * 0.76f, size.height * 0.32f)
                }
                drawPath(p, tick, style = Stroke(strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }
}

/** The small "In use" badge: accent fill, black text. */
@Composable
fun Badge(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(AuraDockRadii.pill))
            .background(AuraDockColors.Accent)
            .padding(horizontal = AuraDockSpacing.smd, vertical = AuraDockSpacing.xs)
    ) {
        Text(text = text, style = AuraDockTypography.Caption, color = AuraDockColors.OnAccent)
    }
}

/** Progress dots for a walkthrough: the current step is a longer accent pill, the rest are small grey dots. */
@Composable
fun ProgressDots(count: Int, current: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(AuraDockSpacing.sm), verticalAlignment = Alignment.CenterVertically) {
        repeat(count) { i ->
            val w by animateDpAsState(if (i == current) AuraDockSizes.dotCurrent else AuraDockSizes.dot, AuraDockMotion.softSpring(), label = "dotW")
            val c by animateColorAsState(if (i == current) AuraDockColors.Accent else AuraDockColors.Inactive, label = "dotC")
            Box(Modifier.size(w, AuraDockSizes.dot).clip(CircleShape).background(c))
        }
    }
}

/**
 * Blur to focus: new content goes from blur 12 to 0, opacity 0 to 1 and a 4 dp rise in 260 ms. [index] staggers a list by 40 ms a row.
 * (Blur only shows on Android 12 and up; older phones get the fade and the rise.)
 */
@Composable
fun BlurIn(modifier: Modifier = Modifier, index: Int = 0, content: @Composable () -> Unit) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay((index * AuraDockMotion.Stagger).toLong())
        progress.animateTo(1f, AuraDockMotion.tween(AuraDockMotion.BlurToFocus, AuraDockMotion.EasingEmphasized))
    }
    val p = progress.value
    Box(
        modifier = modifier
            .graphicsLayer { translationY = (1f - p) * AuraDockMotion.BlurToFocusRiseDp * density }
            .alpha(p)
            .then(if (p < 1f) Modifier.blur((AuraDockMotion.BlurToFocusRadiusDp * (1f - p)).dp) else Modifier)
    ) { content() }
}

@PreviewComponent
@Composable
private fun ControlsPreview() {
    AuraDockTheme {
        Column(Modifier.background(AuraDockColors.Background).padding(AuraDockSpacing.mdl), verticalArrangement = Arrangement.spacedBy(AuraDockSpacing.md)) {
            PrimaryPill("Dock now", {})
            Row(horizontalArrangement = Arrangement.spacedBy(AuraDockSpacing.smd)) { QuietPill("Allow", {}); GreyTextButton("Skip for now", {}) }
            Row(horizontalArrangement = Arrangement.spacedBy(AuraDockSpacing.smd), verticalAlignment = Alignment.CenterVertically) {
                AuraSwitch(true, {}); AuraSwitch(false, {}); AuraCheckbox(true); AuraCheckbox(false); AuraCheckbox(false, dimmed = true); Badge("In use")
            }
            ProgressDots(5, 1)
        }
    }
}
