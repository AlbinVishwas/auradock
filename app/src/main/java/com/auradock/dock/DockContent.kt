package com.auradock.dock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import com.auradock.ui.widgets.ClockWidget

/**
 * What the dock looks like for [template]. This is the one place that draws the dock: the screensaver, the
 * in-app dock, the dashboard thumbnail and the gallery all call it, so they cannot look different.
 *
 * It is "stateless": it is handed the time and draws. The live version that watches the clock and the saved
 * template is [DockScreen].
 */
@Composable
fun DockContent(template: DockTemplate, timeMillis: Long, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        when (template) {
            DockTemplate.MINIMAL -> ClockWidget(timeMillis = timeMillis, dimmed = true, showDate = false)
            // FOCUS and PRODUCTIVITY are built in step 7; until then anything else draws Classic.
            else -> ClassicLayout(timeMillis)
        }
    }
}

/** Big clock, date, weather, on a glass card. */
@Composable
private fun ClassicLayout(timeMillis: Long) {
    GlassCard(modifier = Modifier.fillMaxSize().padding(32.dp)) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically)
        ) {
            ClockWidget(timeMillis = timeMillis)
            WeatherLine(temperatureLabel = null)
        }
    }
}

/** Temperature, or an em dash while there is no real weather (step 8 supplies it). Never fake data. */
@Composable
private fun WeatherLine(temperatureLabel: String?) {
    Text(
        text = temperatureLabel ?: "—",
        color = AuraDockColors.OnSurfaceVariant,
        fontSize = 28.sp,
        fontWeight = FontWeight.Light
    )
}
