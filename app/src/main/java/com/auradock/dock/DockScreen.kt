package com.auradock.dock

import android.util.Log
import android.view.Window
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.auradock.ui.components.BurnInProtection
import com.auradock.weather.usableAt

/** Screen brightness (0..1) for the dimmed Minimal template. */
private const val MINIMAL_BRIGHTNESS = 0.05f

/**
 * The live dock: reads the saved template and the minute clock from [DockData], draws [DockContent], keeps it
 * drifting slowly ([BurnInProtection], so no pixel stays lit in the same place on an OLED), and dims the
 * screen for Minimal.
 *
 * Used by BOTH the screensaver and the in-app dock, passing their own [window]. Because there is one
 * composable, they look identical by construction.
 */
@Composable
fun DockScreen(dockData: DockData, window: Window?, modifier: Modifier = Modifier) {
    val template by dockData.selectedTemplate.collectAsState()
    val time by dockData.timeMillis.collectAsState(initial = dockData.currentTimeMillis())
    val weather by dockData.weather.collectAsState()

    // The dock just appeared: refresh the weather if it is 30+ minutes old (does nothing otherwise).
    LaunchedEffect(Unit) { dockData.refreshWeather() }

    DockBrightness(window, template)

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(visible = visible, enter = fadeIn(animationSpec = tween(320))) {
        BurnInProtection(modifier = modifier) { drift ->
            DockContent(
                template = template,
                state = DockUiState(timeMillis = time, weather = weather.usableAt(time)),
                modifier = drift.fillMaxSize()
            )
        }
    }
}

/**
 * Sets the window's brightness while the dock is showing: very low for Minimal, the user's normal setting
 * for the others, and puts back what it found when the dock leaves.
 */
@Composable
private fun DockBrightness(window: Window?, template: DockTemplate) {
    DisposableEffect(window, template) {
        val params = window?.attributes
        val original = params?.screenBrightness
        if (window != null && params != null) {
            params.screenBrightness =
                if (template == DockTemplate.MINIMAL) MINIMAL_BRIGHTNESS
                else WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            window.attributes = params
            Log.d("DockScreen", "template=$template window brightness=${window.attributes.screenBrightness}")
        }
        onDispose {
            if (window != null && params != null && original != null) {
                params.screenBrightness = original
                window.attributes = params
            }
        }
    }
}
