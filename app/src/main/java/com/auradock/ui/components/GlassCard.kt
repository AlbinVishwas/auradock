package com.auradock.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.auradock.ui.theme.AuraDockColors
import com.auradock.ui.theme.AuraDockSpacing

/**
 * # GlassCard
 *
 * Canonical glassmorphic container for all AuraDock standby widgets.
 * Extracted (Step 3) from the inline definitions in [WeatherWidget] and
 * [CalendarWidget] to enforce a single source of truth for card styling.
 *
 * ## Visual spec
 * | Property       | Value            |
 * |----------------|------------------|
 * | Corner radius  | 28 dp            |
 * | Fill gradient  | white 9% → 4%    |
 * | Border         | white 12% alpha  |
 * | Content pad    | 20 dp (default)  |
 *
 * ## Usage
 * ```kotlin
 * GlassCard(modifier = Modifier.weight(1f).fillMaxHeight()) {
 *     WeatherContent(...)
 * }
 * ```
 *
 * @param modifier      Caller-supplied sizing / positioning modifier.
 * @param cornerRadius  Override corner radius if needed (default 28 dp).
 * @param padding       Internal content padding (default 20 dp).
 * @param content       Slot for widget content.
 */
@Composable
fun GlassCard(
    modifier     : Modifier  = Modifier,
    cornerRadius : Dp        = 28.dp,
    padding      : Dp        = AuraDockSpacing.CardPadding,
    content      : @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val glassTop = AuraDockColors.GlassCardTop
    val glassBottom = AuraDockColors.GlassCardBottom

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        glassTop,
                        glassBottom
                    )
                )
            )
            .border(
                width = 1.dp,
                color = AuraDockColors.GlassBorder,        // white 12%
                shape = shape
            )
            .padding(padding),
        content = content
    )
}
