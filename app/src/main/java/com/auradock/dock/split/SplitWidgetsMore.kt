package com.auradock.dock.split

import android.text.format.DateFormat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.layout
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.auradock.data.nextEvent
import com.auradock.data.upcomingEvents
import com.auradock.dock.boldTimeString
import com.auradock.dock.BoldClock
import com.auradock.dock.world.GeoPoint
import com.auradock.dock.world.Solar
import com.auradock.dock.world.WorldCities
import com.auradock.dock.world.WorldHalfView
import com.auradock.dock.world.drawHalfDot
import com.auradock.dock.world.drawLandHalf
import com.auradock.dock.world.drawTerminatorHalf
import com.auradock.dock.world.WorldProjection
import com.auradock.tasks.isOverdue
import com.auradock.tasks.priorityOrder
import com.auradock.ui.theme.AuraDockColors
import com.auradock.ui.theme.AuraDockSplit
import com.auradock.ui.theme.AuraDockTypography
import com.auradock.ui.theme.AuraDockWorld
import com.auradock.ui.weather.WeatherIcon
import com.auradock.weather.temperatureLabel
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * Sets a big number on its own line box the way the frames' CSS does: the box is [lineEm] of the font size tall (0.8), and the baseline sits
 * [baselineEm] above its bottom, whatever the font's own ascent and descent. Compose's line-height trimming leaves the font's descent below
 * big digits, which would push the layout down.
 */
fun Modifier.dockLine(fontSize: androidx.compose.ui.unit.TextUnit, lineEm: Float = 0.8f, baselineEm: Float = 0.072f): Modifier =
    this.layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val fontPx = fontSize.toPx()
        val box = (fontPx * lineEm).roundToInt()
        val baseline = placeable[androidx.compose.ui.layout.LastBaseline]
        layout(placeable.width, box) { placeable.place(0, box - (fontPx * baselineEm).roundToInt() - baseline) }
    }

/** Bold time: the Bold clock's digits, as large as the half allows (no extra margin: the half is its own safe area). */
@Composable
fun BoldTimeWidget(env: WidgetEnv, modifier: Modifier = Modifier) {
    BoldClock(env.state.timeMillis, env.use24Hour, modifier, palette = env.state.boldPalette, safeSide = 0.dp, safeVertical = 0.dp)
}

/** Digital: the time in Host Grotesk (shrunk to fit the half if it is wider than the half), and the date under it. */
@Composable
fun DigitalWidget(env: WidgetEnv, modifier: Modifier = Modifier) {
    val date = remember(env.state.timeMillis / 60_000L) {
        DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault()).format(Instant.ofEpochMilli(env.state.timeMillis).atZone(ZoneId.systemDefault()))
    }
    val time = boldTimeString(env.state.timeMillis, env.use24Hour)
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    androidx.compose.foundation.layout.BoxWithConstraints(modifier.fillMaxSize()) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        val natural = measurer.measure(time, AuraDockTypography.SplitDigital, maxLines = 1).size.width
        val room = with(density) { (maxWidth - 8.dp).toPx() }
        val k = if (natural > room) room / natural else 1f
        val base = AuraDockTypography.SplitDigital
        val fitted = base.copy(fontSize = base.fontSize * k, lineHeight = base.lineHeight * k, letterSpacing = base.letterSpacing * k)
        Column(Modifier.fillMaxSize().padding(start = 8.dp), verticalArrangement = Arrangement.Center) {
            Text(time, style = fitted, color = env.tones.primary, maxLines = 1, softWrap = false, modifier = Modifier.dockLine(fitted.fontSize))
            Text(date, style = AuraDockTypography.SplitDigitalDate, color = env.tones.secondary, modifier = Modifier.padding(top = 22.dp))
        }
    }
}

/** World: the half-size map with the day and night line, home in coral, and the home city's name and time. */
@Composable
fun WorldWidget(env: WidgetEnv, modifier: Modifier = Modifier) {
    val state = env.state
    val zone = remember { ZoneId.systemDefault() }
    val fallback = remember(zone, state.timeMillis / 3_600_000L) { WorldCities.forZone(zone, state.timeMillis) }
    val home: GeoPoint = state.world.home ?: fallback.point
    val sun = remember(state.timeMillis / AuraDockWorld.RefreshMillis) { Solar.at(state.timeMillis / AuraDockWorld.RefreshMillis * AuraDockWorld.RefreshMillis) }
    val label = state.weather?.city?.takeIf { it.isNotBlank() } ?: fallback.name
    val dayColor = AuraDockColors.WorldDay
    val nightColor = AuraDockColors.WorldNight
    val lineColor = AuraDockColors.WorldLine
    val stage = AuraDockColors.Background
    val shift = WorldHalfView.shiftX(home.lon)
    Box(modifier.fillMaxSize()) {
        Canvas(Modifier.size(WorldHalfView.WIDTH.dp, WorldHalfView.HEIGHT.dp)) {
            val u = 1.dp.toPx()
            drawLandHalf(u, shift, sun, dayColor, nightColor)
            drawTerminatorHalf(u, shift, sun, lineColor)
            state.world.extras.forEach { c ->
                drawHalfDot((WorldProjection.x(c.lon) + shift) * u, (WorldProjection.y(c.lat) + WorldHalfView.SHIFT_Y) * u, AuraDockWorld.CityDotRadius * u, androidx.compose.ui.graphics.Color.White, stage, u)
            }
            if (state.world.home != null) {
                drawHalfDot((WorldProjection.x(home.lon) + shift) * u, (WorldProjection.y(home.lat) + WorldHalfView.SHIFT_Y) * u, 7f * u, AuraDockColors.Coral, stage, u, ring = AuraDockColors.Coral.copy(alpha = 0.4f))
            }
        }
        Column(Modifier.align(Alignment.BottomStart), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(label, style = AuraDockTypography.SplitWorldCity, color = AuraDockColors.Coral, maxLines = 1)
            Text(boldTimeString(state.timeMillis, env.use24Hour), style = AuraDockTypography.SplitWorldTime, color = env.tones.primary, maxLines = 1, modifier = Modifier.dockLine(AuraDockTypography.SplitWorldTime.fontSize))
        }
    }
}

/** Big date: "Fri Nov" and the day number in a huge light weight. */
@Composable
fun BigDateWidget(env: WidgetEnv, modifier: Modifier = Modifier) {
    val t = remember(env.state.timeMillis / 60_000L) { Instant.ofEpochMilli(env.state.timeMillis).atZone(ZoneId.systemDefault()) }
    val locale = Locale.getDefault()
    Column(modifier.fillMaxSize().padding(start = 8.dp), verticalArrangement = Arrangement.Center) {
        Row {
            Text(DateTimeFormatter.ofPattern("EEE", locale).format(t), style = AuraDockTypography.SplitBigDateHead, color = AuraDockColors.Coral.copy(alpha = env.tones.accentAlpha))
            Text(" " + DateTimeFormatter.ofPattern("MMM", locale).format(t), style = AuraDockTypography.SplitBigDateHead, color = env.tones.tertiary)
        }
        Text(
            t.dayOfMonth.toString(), style = AuraDockTypography.SplitBigDateNumber, color = env.tones.primary, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 18.dp).dockLine(AuraDockTypography.SplitBigDateNumber.fontSize), maxLines = 1
        )
    }
}

/** "in 25 min" or "in 1 h 5 min". */
fun countdownText(startMillis: Long, nowMillis: Long): String {
    val minutes = ceil((startMillis - nowMillis) / 60_000.0).toInt().coerceAtLeast(0)
    return if (minutes < 60) "in $minutes min" else "in ${minutes / 60} h" + (if (minutes % 60 != 0) " ${minutes % 60} min" else "")
}

/** Up next: the next event (its time, title and how long, with where it is), and the one after it. Reuses the dock's next-event logic. */
@Composable
fun UpNextWidget(env: WidgetEnv, modifier: Modifier = Modifier) {
    val now = env.state.timeMillis
    val context = LocalContext.current
    val timeFormat = remember(context) { DateFormat.getTimeFormat(context) }
    val next = nextEvent(env.state.events, now)
    val then = upcomingEvents(env.state.events, now).drop(1).firstOrNull()
    Column(modifier.fillMaxSize().padding(start = 8.dp), verticalArrangement = Arrangement.Center) {
        Text("Up next", style = AuraDockTypography.SplitUpNextLabel, color = env.tones.tertiary)
        if (next == null) {
            Text("Nothing else today", style = AuraDockTypography.SplitUpNextTitle, color = env.tones.primary, modifier = Modifier.padding(top = 14.dp))
        } else {
            Text(timeFormat.format(Date(next.startTime)), style = AuraDockTypography.SplitUpNextTime, color = AuraDockColors.Coral.copy(alpha = env.tones.accentAlpha), modifier = Modifier.padding(top = 14.dp))
            Text(next.title, style = AuraDockTypography.SplitUpNextTitle, color = env.tones.primary, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
            Text(
                listOfNotNull(countdownText(next.startTime, now), next.location).joinToString(" · "),
                style = AuraDockTypography.SplitUpNextSub, color = env.tones.secondary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp)
            )
            if (then != null) {
                Row(Modifier.padding(top = 40.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
                    Text(timeFormat.format(Date(then.startTime)), style = AuraDockTypography.SplitUpNextThen, color = env.tones.secondary)
                    Text(then.title, style = AuraDockTypography.SplitUpNextThenTitle, color = env.tones.tertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

/** Weather: where, the temperature in Host Grotesk, the sky, and today's high and low. */
@Composable
fun WeatherWidget(env: WidgetEnv, modifier: Modifier = Modifier) {
    val weather = env.state.weather
    Column(modifier.fillMaxSize().padding(start = 8.dp), verticalArrangement = Arrangement.Center) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.NearMe, contentDescription = null, tint = env.tones.secondary, modifier = Modifier.size(18.dp))
            Text(weather?.city ?: "Weather", style = AuraDockTypography.SplitWeatherCity, color = env.tones.secondary, maxLines = 1)
        }
        Text(weather?.let { temperatureLabel(it.temperatureC) } ?: "—", style = AuraDockTypography.SplitWeatherTemp, color = env.tones.primary, modifier = Modifier.padding(top = 22.dp).dockLine(AuraDockTypography.SplitWeatherTemp.fontSize), maxLines = 1)
        if (weather != null) {
            Row(Modifier.padding(top = 26.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                WeatherIcon(weather.condition.icon, env.tones.primary, size = 34.dp)
                Text(weather.condition.label, style = AuraDockTypography.SplitWeatherCondition, color = env.tones.primary, maxLines = 1)
            }
            val hi = weather.highC
            val lo = weather.lowC
            if (hi != null && lo != null) {
                Text("H ${temperatureLabel(hi)} · L ${temperatureLabel(lo)}", style = AuraDockTypography.SplitWeatherRange, color = env.tones.secondary, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

/** Tasks: what is left today, overdue first, with a ring to tick each one. Reuses the dock's task order. */
@Composable
fun TasksWidget(env: WidgetEnv, modifier: Modifier = Modifier) {
    val now = env.state.timeMillis
    val context = LocalContext.current
    val timeFormat = remember(context) { DateFormat.getTimeFormat(context) }
    val open = priorityOrder(env.state.tasks, now)
    Column(modifier.fillMaxSize().padding(horizontal = 8.dp), verticalArrangement = Arrangement.Center) {
        Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Text("Today", style = AuraDockTypography.SplitTasksTitle, color = env.tones.primary)
            Text(if (open.isEmpty()) "All done" else "${open.size} left", style = AuraDockTypography.SplitTasksCount, color = env.tones.tertiary)
        }
        open.take(AuraDockSplit.TaskRows).forEachIndexed { i, task ->
            val overdue = isOverdue(task, now)
            if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(AuraDockColors.SplitCard))
            Row(
                Modifier.fillMaxWidth().padding(vertical = AuraDockSplit.TaskRowPad),
                horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(AuraDockSplit.CheckSize).border(AuraDockSplit.CheckStroke, if (overdue) AuraDockColors.Coral else AuraDockColors.Outline, CircleShape)
                        .clip(CircleShape).semantics { contentDescription = "Mark ${task.title} as done" }
                        .clickable(role = Role.Button) { env.actions.onTaskDone(task.id) }
                )
                Column(Modifier.weight(1f)) {
                    Text(task.title, style = AuraDockTypography.SplitTask, color = env.tones.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (overdue) Text("Overdue · " + (task.dueAt?.let { timeFormat.format(Date(it)) } ?: ""), style = AuraDockTypography.SplitTaskSub, color = AuraDockColors.Coral.copy(alpha = env.tones.accentAlpha), modifier = Modifier.padding(top = 3.dp))
                }
                if (!overdue) task.dueAt?.let { Text(timeFormat.format(Date(it)), style = AuraDockTypography.SplitTaskTime, color = env.tones.secondary) }
            }
        }
    }
}

/** "42:10", or "1:02:10" once past an hour. */
fun focusClock(elapsedMillis: Long): String {
    val s = (elapsedMillis / 1000).coerceAtLeast(0)
    return if (s >= 3600) "%d:%02d:%02d".format(s / 3600, (s / 60) % 60, s % 60) else "%d:%02d".format(s / 60, s % 60)
}

/** Focus: a ring for the hour, the session's time in it, and how many notifications were held. Ticks every second while showing. */
@Composable
fun FocusWidget(env: WidgetEnv, modifier: Modifier = Modifier) {
    val session = env.state.session
    val now by produceState(env.state.timeMillis, session?.startedAt) {
        while (true) {
            value = System.currentTimeMillis()
            delay(1000L - value % 1000L)
        }
    }
    val elapsed = if (session == null) 0L else now - session.startedAt
    val progress = (elapsed.toFloat() / AuraDockSplit.FocusLapMillis).coerceIn(0f, 1f)
    val track = AuraDockColors.Field
    val arc = AuraDockColors.Coral.copy(alpha = env.tones.accentAlpha)
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(Modifier.size(AuraDockSplit.FocusSize), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val u = 1.dp.toPx()
                val r = AuraDockSplit.FocusRingRadius * u
                val c = Offset(size.width / 2f, size.height / 2f)
                val stroke = AuraDockSplit.FocusRingStroke * u
                drawCircle(track, r, c, style = Stroke(stroke))
                drawArc(arc, -90f, 360f * progress, false, Offset(c.x - r, c.y - r), Size(2 * r, 2 * r), style = Stroke(stroke, cap = StrokeCap.Round))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Focus", style = AuraDockTypography.SplitFocusLabel, color = env.tones.secondary)
                Text(focusClock(elapsed), style = AuraDockTypography.SplitFocusTime, color = env.tones.primary, modifier = Modifier.padding(top = 8.dp).dockLine(AuraDockTypography.SplitFocusTime.fontSize, lineEm = 0.9f, baselineEm = 0.1f))
                Text("${session?.held ?: 0} held", style = AuraDockTypography.SplitFocusLabel, color = env.tones.secondary, modifier = Modifier.padding(top = 10.dp))
            }
        }
    }
}

/**
 * Now playing: album art, title and artist, the playing app's own icon, a play/pause button and a progress line. Drag the card sideways for the
 * next or previous track. The widget is not in its stack while nothing plays.
 */
@Composable
fun NowPlayingWidget(env: WidgetEnv, modifier: Modifier = Modifier) {
    val np = env.state.split.nowPlaying ?: return
    val controls = env.actions.media
    val progress by produceState(np.progress(android.os.SystemClock.elapsedRealtime()), np) {
        while (true) {
            value = np.progress(android.os.SystemClock.elapsedRealtime())
            delay(1000L)
        }
    }
    val skipPx = with(androidx.compose.ui.platform.LocalDensity.current) { AuraDockSplit.SkipDistance.toPx() }
    Box(
        modifier.fillMaxSize().clip(RoundedCornerShape(AuraDockSplit.CardRadius)).background(AuraDockColors.SplitCard)
            .pointerInput(np.title) {
                var dx = 0f
                detectHorizontalDragGestures(
                    onDragStart = { dx = 0f },
                    onHorizontalDrag = { change, delta -> dx += delta; change.consume() },
                    onDragEnd = { if (dx <= -skipPx) controls.next() else if (dx >= skipPx) controls.previous() }
                )
            }
    ) {
        Box(Modifier.padding(AuraDockSplit.ArtInset).size(AuraDockSplit.ArtSize).clip(RoundedCornerShape(AuraDockSplit.ArtRadius)).background(AuraDockColors.SplitArt)) {
            val art = np.art
            if (art != null) Image(art.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            else Canvas(Modifier.fillMaxSize()) {
                val k = size.width / 210f
                listOf(72f, 52f, 32f).forEach { drawCircle(AuraDockColors.Inactive, it * k, center, style = Stroke(1.5f * k)) }
                drawCircle(AuraDockColors.Outline, 9f * k, center)
            }
        }
        Box(
            Modifier.align(Alignment.TopEnd).padding(AuraDockSplit.ArtInset).size(AuraDockSplit.AppBadge).clip(CircleShape).background(AuraDockColors.DockText)
                .semantics { contentDescription = np.appName },
            contentAlignment = Alignment.Center
        ) {
            val icon = np.appIcon
            if (icon != null) Image(icon.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            else Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = AuraDockColors.Background, modifier = Modifier.size(24.dp))
        }
        Column(Modifier.align(Alignment.BottomStart).padding(start = AuraDockSplit.TitleInset, bottom = AuraDockSplit.TitleBottom).widthIn(max = AuraDockSplit.TitleMaxWidth)) {
            Text(np.title, style = AuraDockTypography.SplitTrack, color = env.tones.primary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (np.artist.isNotBlank()) Text(np.artist, style = AuraDockTypography.SplitArtist, color = env.tones.tertiary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
        }
        Box(
            Modifier.align(Alignment.BottomEnd).padding(end = AuraDockSplit.ArtInset, bottom = 30.dp).size(AuraDockSplit.PlayButton).clip(CircleShape).background(AuraDockColors.DockText)
                .semantics { contentDescription = if (np.playing) "Pause" else "Play" }
                .clickable(role = Role.Button) { controls.playPause() },
            contentAlignment = Alignment.Center
        ) {
            Icon(if (np.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, contentDescription = null, tint = AuraDockColors.Background, modifier = Modifier.size(34.dp))
        }
        Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(AuraDockSplit.ProgressHeight).background(AuraDockColors.Field)) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(progress).background(AuraDockColors.Outline))
        }
    }
}
