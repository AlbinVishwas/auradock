package com.auradock.ui.tasks

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import com.auradock.ui.theme.AuraDockColors
import com.auradock.ui.theme.AuraDockRadii
import com.auradock.ui.theme.AuraDockSizes
import com.auradock.ui.theme.AuraDockSpacing
import com.auradock.ui.theme.AuraDockTypography

/** The minutes the wheel offers: every five. A task opened at 11:07 starts on the nearest of them. */
const val WHEEL_MINUTE_STEP = 5

/**
 * The time picker of the handoff: three wheels (hour, minute, AM/PM; two without AM/PM on a 24-hour phone) on a rounded field,
 * a band behind the chosen row, the chosen number large and the rows either side small and grey. [onChange] gets the hour (0 to
 * 23) and the minute whenever a wheel settles.
 */
@Composable
fun TimeWheel(hour: Int, minute: Int, is24Hour: Boolean, onChange: (hour: Int, minute: Int) -> Unit, modifier: Modifier = Modifier) {
    val minuteIndex = ((minute + WHEEL_MINUTE_STEP / 2) / WHEEL_MINUTE_STEP) % (60 / WHEEL_MINUTE_STEP)
    val pm = hour >= 12
    val hourValue = if (is24Hour) hour else (hour % 12).let { if (it == 0) 12 else it }
    val label = "%d:%02d%s".format(hourValue, minuteIndex * WHEEL_MINUTE_STEP, if (is24Hour) "" else if (pm) " PM" else " AM")

    fun emit(newHourValue: Int = hourValue, newMinuteIndex: Int = minuteIndex, newPm: Boolean = pm) {
        val h24 = if (is24Hour) newHourValue else (newHourValue % 12) + if (newPm) 12 else 0
        onChange(h24, newMinuteIndex * WHEEL_MINUTE_STEP)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(AuraDockSizes.wheelHeight)
            .clip(RoundedCornerShape(AuraDockRadii.field))
            .background(AuraDockColors.Field)
            .semantics { contentDescription = "Time picker, $label" }
    ) {
        Box(
            Modifier
                .align(Alignment.Center)
                .padding(horizontal = AuraDockSizes.wheelHighlightInset)
                .fillMaxWidth()
                .height(AuraDockSizes.wheelRow - AuraDockSpacing.xxs - AuraDockSpacing.xxs)
                .clip(RoundedCornerShape(AuraDockRadii.control))
                .background(AuraDockColors.WheelHighlight)
        )
        Row(modifier = Modifier.fillMaxSize()) {
            if (is24Hour) {
                Wheel(24, loop = true, selected = hour, text = { "%02d".format(it) }, onSelected = { emit(newHourValue = it) }, modifier = Modifier.weight(1f))
            } else {
                Wheel(12, loop = true, selected = hourValue - 1, text = { (it + 1).toString() }, onSelected = { emit(newHourValue = it + 1) }, modifier = Modifier.weight(1f))
            }
            Wheel(60 / WHEEL_MINUTE_STEP, loop = true, selected = minuteIndex, text = { "%02d".format(it * WHEEL_MINUTE_STEP) }, onSelected = { emit(newMinuteIndex = it) }, modifier = Modifier.weight(1f))
            if (!is24Hour) {
                Wheel(2, loop = false, selected = if (pm) 1 else 0, text = { if (it == 0) "AM" else "PM" }, onSelected = { emit(newPm = it == 1) }, chosenStyle = AuraDockTypography.WheelChosenSmall, modifier = Modifier.weight(1f))
            }
        }
    }
}

private const val REPEATS = 200

/** One wheel: snaps a row to the middle and reports it. A looping wheel repeats its numbers so it never runs out. */
@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun Wheel(
    count: Int,
    loop: Boolean,
    selected: Int,
    text: (Int) -> String,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    chosenStyle: TextStyle = AuraDockTypography.WheelChosen
) {
    val rowPx = with(LocalDensity.current) { AuraDockSizes.wheelRow.toPx() }
    val start = if (loop) (REPEATS / 2) * count + selected else selected
    val state = rememberLazyListState(initialFirstVisibleItemIndex = start)
    val centre by remember { derivedStateOf { state.firstVisibleItemIndex + if (state.firstVisibleItemScrollOffset > rowPx / 2) 1 else 0 } }
    LaunchedEffect(state) {
        snapshotFlow { state.isScrollInProgress to centre }.collect { (scrolling, index) -> if (!scrolling) onSelected(index % count) }
    }
    val edge = (AuraDockSizes.wheelHeight - AuraDockSizes.wheelRow) / 2
    LazyColumn(
        state = state,
        flingBehavior = rememberSnapFlingBehavior(state),
        contentPadding = PaddingValues(vertical = edge),
        modifier = modifier.fillMaxSize()
    ) {
        items(if (loop) count * REPEATS else count) { index ->
            val chosen = index == centre
            Box(Modifier.fillMaxWidth().height(AuraDockSizes.wheelRow), contentAlignment = Alignment.Center) {
                Text(
                    text = text(index % count),
                    style = if (chosen) chosenStyle else AuraDockTypography.WheelOther,
                    color = if (chosen) AuraDockColors.OnBackground else AuraDockColors.WheelDim
                )
            }
        }
    }
}
