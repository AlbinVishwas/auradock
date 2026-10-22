package com.auradock.ui.widgets

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auradock.ui.theme.AuraDockColors
import com.auradock.ui.theme.AuraDockSpacing
import com.auradock.ui.theme.AuraDockTypography
import com.auradock.ui.components.GlassCard

/**
 * # WeatherWidget
 *
 * Glassmorphic card displaying current weather data.
 *
 * ## Visual Design
 * - Translucent dark card with hairline white border
 * - Oversized temperature value for glanceability at 1–2m
 * - Condition icon rendered from [iconRes] with amber glow
 * - City name in muted caption below
 *
 * ## Data Binding
 * Receives a pre-resolved [WeatherUiState] from the ViewModel layer.
 * Icon resources should be mapped from OWM icon codes before this composable.
 *
 * @param state    Resolved weather state — see [WeatherUiState].
 * @param modifier Caller-supplied layout modifier.
 */
@Composable
fun WeatherWidget(
    state    : WeatherUiState,
    modifier : Modifier = Modifier
) {
    GlassCard(modifier = modifier.padding(horizontal = 4.dp)) {
        Column(
            verticalArrangement   = Arrangement.SpaceBetween,
            modifier              = Modifier.fillMaxSize()
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AuraDockSpacing.md)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter            = painterResource(id = state.iconRes),
                            contentDescription = state.condition,
                            tint               = AuraDockColors.AccentAmber.copy(alpha = 0.4f),
                            modifier           = Modifier.size(52.dp).blur(8.dp)
                        )
                        Icon(
                            painter            = painterResource(id = state.iconRes),
                            contentDescription = state.condition,
                            tint               = AuraDockColors.AccentAmber,
                            modifier           = Modifier.size(44.dp)
                        )
                    }

                    Text(
                        text  = state.temperatureLabel,
                        style = AuraDockTypography.WidgetPrimary.copy(
                            color    = Color.White,
                            fontSize = 42.sp
                        )
                    )
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    text  = state.condition,
                    style = AuraDockTypography.WidgetLabel.copy(
                        color = Color.White
                    )
                )
            }

            Column {
                Text(
                    text  = state.cityName,
                    style = AuraDockTypography.Caption.copy(
                        color       = AuraDockColors.OnSurfaceVariant,
                        letterSpacing = 2.sp
                    )
                )
            }
        }
    }
}

/**
 * Resolved UI state for [WeatherWidget] — produced by the ViewModel mapping
 * [com.auradock.widget.WidgetData.Weather] to icon resources.
 */
data class WeatherUiState(
    val temperatureLabel : String,  // e.g. "22°"
    val condition        : String,  // e.g. "Partly Cloudy"
    val cityName         : String,  // e.g. "BANGALORE"
    val iconRes          : Int,     // resolved drawable res from iconCode
)

// Step 3: GlassCard moved to GlassCard.kt — single source of truth.
