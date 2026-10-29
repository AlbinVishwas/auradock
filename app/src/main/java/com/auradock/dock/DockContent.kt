package com.auradock.dock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auradock.ui.components.GlassCard
import com.auradock.ui.theme.AuraDockColors
import com.auradock.ui.weather.WeatherIcon
import com.auradock.ui.widgets.ClockWidget
import com.auradock.weather.Weather
import com.auradock.weather.conditionLine
import com.auradock.weather.temperatureLabel

/**
 * What the dock looks like for [template]. This is the one place that draws the dock: the screensaver, the
 * in-app dock, the dashboard thumbnail and the gallery all call it, so they cannot look different.
 *
 * It is "stateless": it is handed the time and draws. The live version that watches the clock and the saved
 * template is [DockScreen].
 */
@Composable
fun DockContent(template: DockTemplate, state: DockUiState, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        when (template) {
            DockTemplate.MINIMAL -> ClockWidget(timeMillis = state.timeMillis, dimmed = true, showDate = false)
            // FOCUS and PRODUCTIVITY are built in step 7; until then anything else draws Classic.
            else -> ClassicLayout(state)
        }
    }
}

/** Big clock, date, weather, on a glass card. */
@Composable
private fun ClassicLayout(state: DockUiState) {
    GlassCard(modifier = Modifier.fillMaxSize().padding(32.dp)) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically)
        ) {
            ClockWidget(timeMillis = state.timeMillis)
            WeatherLine(weather = state.weather)
        }
    }
}

/** Icon, temperature and condition (with the city if known), or a quiet dash when there is no weather. Never fake data. */
@Composable
private fun WeatherLine(weather: Weather?) {
    if (weather == null) {
        Text(text = "—", color = AuraDockColors.OnSurfaceVariant, fontSize = 28.sp, fontWeight = FontWeight.Light)
        return
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        WeatherIcon(weather.condition.icon, AuraDockColors.OnSurfaceVariant, size = 44.dp)
        Text(
            text = temperatureLabel(weather.temperatureC),
            color = Color.White,
            fontSize = 40.sp,
            fontWeight = FontWeight.Light
        )
        Text(
            text = conditionLine(weather),
            color = AuraDockColors.OnSurfaceVariant,
            fontSize = 20.sp,
            fontWeight = FontWeight.Light
        )
    }
}
