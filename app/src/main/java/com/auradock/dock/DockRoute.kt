package com.auradock.dock

import kotlinx.coroutines.launch
import com.auradock.ui.theme.AuraDockMotion
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
import com.auradock.dock.world.WorldUiState
import androidx.compose.runtime.collectAsState
import com.auradock.ui.components.BurnInProtection
import com.auradock.dock.lines.DockLineActions
import com.auradock.dock.lines.DockLines
import com.auradock.data.meetingCountdown
import com.auradock.intention.intentionLine
import com.auradock.intention.RESUME_SHOWN_MS
import com.auradock.intention.needsOffer
import com.auradock.intention.resumeTitle
import androidx.compose.runtime.produceState
import kotlinx.coroutines.delay
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
fun DockScreen(dockData: DockData, window: Window?, modifier: Modifier = Modifier, interactive: Boolean = false, touch: DockTouch? = null) {
    val template by dockData.selectedTemplate.collectAsState()
    val boldPalette by dockData.boldPalette.collectAsState()
    val worldHome by dockData.worldHome.collectAsState()
    val worldCities by dockData.worldCities.collectAsState()
    val splitStacks by dockData.splitStacks.collectAsState()
    val monthEventDays by dockData.monthEventDays.collectAsState(initial = emptySet())
    val nowPlaying by dockData.nowPlaying.collectAsState()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val time by dockData.timeMillis.collectAsState(initial = dockData.currentTimeMillis())
    val weather by dockData.weather.collectAsState()
    val events by dockData.todayEvents.collectAsState(initial = emptyList())
    val tasks by dockData.todayTasks.collectAsState(initial = emptyList())
    val notifications by dockData.stripNotifications.collectAsState(initial = emptyList())
    val showText by dockData.showMessageText.collectAsState()
    val session by dockData.session.collectAsState()
    val intention by dockData.intention.collectAsState(initial = null)
    val intentionEntry = intention?.entry
    val intentionNow = intentionLine(intentionEntry)

    // The first dock of the day asks its question (only where touches work). Once asked it is not asked again today.
    var prompt by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(interactive, intention) {
        val today = intention ?: return@LaunchedEffect
        if (interactive && prompt == null && needsOffer(today.entry)) {
            prompt = today.entry?.text.orEmpty()
            dockData.intentionOffered()
        }
    }

    // The dock just appeared: refresh the weather if it is 30+ minutes old (does nothing otherwise).
    LaunchedEffect(Unit) { dockData.refreshWeather(); dockData.startNowPlaying() }

    // "Pick up: <task>" for the first 30 seconds of a dock (not on the dock that asks the morning question). It follows the
    // session's start, so a screensaver handing over to the app does not start another half minute.
    val sessionStart = session?.startedAt
    var askedThisDock by remember { mutableStateOf(false) }
    LaunchedEffect(prompt) { if (prompt != null) askedThisDock = true }
    val resumeActive by produceState(false, sessionStart) {
        val left = if (sessionStart == null) 0L else sessionStart + RESUME_SHOWN_MS - System.currentTimeMillis()
        value = left > 0
        if (left > 0) { delay(left); value = false }
    }
    val resume = if (resumeActive && !askedThisDock) resumeTitle(intentionNow, tasks, time) else null

    DockBrightness(window, template)

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(visible = visible, enter = fadeIn(animationSpec = tween(AuraDockMotion.DockFade))) {
        BurnInProtection(modifier = modifier) { drift ->
            DockContent(
                template = template,
                state = DockUiState(timeMillis = time, weather = weather.usableAt(time), events = events, tasks = tasks,
                    notifications = notifications, showMessageText = showText,
                    session = session?.let { SessionSnapshot(it.startedAt, it.counts.held, it.counts.through) },
                    lines = DockLines(resume = resume, meeting = meetingCountdown(events, time), intention = intentionNow),
                    intentionPrompt = prompt, boldPalette = boldPalette, world = WorldUiState(worldHome, worldCities), split = SplitUiState(splitStacks, monthEventDays, nowPlaying)),
                actions = DockActions(
                    touch = touch,
                    onStackChange = dockData::setSplitStack,
                    media = dockData.mediaControls,
                    onTaskDone = { id -> scope.launch { dockData.setTaskDone(id, true) } },
                    lines = DockLineActions(onToggleIntention = dockData::setIntentionDone),
                    onSaveIntention = { prompt = null; dockData.saveIntention(it) },
                    onSkipIntention = { prompt = null; dockData.skipIntention() }
                ),
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
