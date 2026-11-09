package com.auradock.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import com.auradock.ui.theme.AuraDockColors
import com.auradock.ui.theme.AuraDockMotion
import com.auradock.ui.theme.AuraDockSizes

/**
 * A title that, when [struck], gets a line drawn across it from left to right while the text fades from [color] to [struckColor].
 * The task list and the dock's morning intention use the same one.
 */
@Composable
fun StrikeText(
    text: String,
    struck: Boolean,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    struckColor: Color = AuraDockColors.Tertiary,
    maxLines: Int = 2
) {
    val progress by animateFloatAsState(if (struck) 1f else 0f, AuraDockMotion.tween(AuraDockMotion.Strike), label = "strike")
    val shown by animateColorAsState(if (struck) struckColor else color, AuraDockMotion.tween(AuraDockMotion.Strike), label = "strikeFade")
    Text(
        text = text,
        style = style,
        color = shown,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.drawWithContent {
            drawContent()
            if (progress > 0f) {
                val y = size.height / 2f
                drawLine(
                    color = struckColor,
                    start = Offset(0f, y),
                    end = Offset(size.width * progress, y),
                    strokeWidth = AuraDockSizes.strikeLine.toPx()
                )
            }
        }
    )
}
