package com.auradock.ui.tasks

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.auradock.tasks.TaskEntity
import com.auradock.tasks.newlyDone
import com.auradock.ui.theme.AuraDockMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The ids of tasks that were ticked within the last 600 ms. They keep their place in the list for that long (so the tick, the strike
 * and the fade are seen where the row is), then the list lets them sink to the bottom. Nothing settles on the first draw.
 */
@Composable
fun rememberSettlingDone(tasks: List<TaskEntity>?): Set<Long> {
    var settling by remember { mutableStateOf(emptySet<Long>()) }
    var previous by remember { mutableStateOf<Set<Long>?>(null) }
    val scope = rememberCoroutineScope()
    val doneNow = tasks.orEmpty().filter { it.done }.map { it.id }.toSet()
    LaunchedEffect(doneNow) {
        val before = previous
        previous = doneNow
        if (before == null) return@LaunchedEffect
        val fresh = newlyDone(before, doneNow)
        if (fresh.isNotEmpty()) {
            settling = settling + fresh
            // Own job per tick: a later change must not cancel the wait of an earlier one.
            scope.launch {
                delay(AuraDockMotion.TaskSink.toLong())
                settling = settling - fresh
            }
        }
        settling = settling.filter { it in doneNow }.toSet() // un-ticked again: nothing to wait for
    }
    return settling
}
