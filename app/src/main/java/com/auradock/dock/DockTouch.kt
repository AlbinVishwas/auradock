package com.auradock.dock

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput

/**
 * What touching the dock does, the same in the app and in the screensaver: a single tap only shows "Undo focus" (when Focus is on), a
 * double tap leaves the dock. Swipe (change widget) and long press (edit) belong to the Split template.
 */
data class DockTouch(val onSingleTap: () -> Unit = {}, val onDoubleTap: () -> Unit = {})

/**
 * Applies the dock's tap rules; with [touch] null (previews, thumbnails, the gallery) it does nothing and takes no touches. The detector is
 * started once and reads the latest callbacks, because the dock recomposes on every frame of its burn-in drift and a detector restarted that
 * often would never see a long press finish.
 */
fun Modifier.dockTouch(touch: DockTouch?, onLongPress: ((Offset) -> Unit)? = null): Modifier =
    if (touch == null) this else composed {
        val latestTouch by rememberUpdatedState(touch)
        val latestLongPress by rememberUpdatedState(onLongPress)
        Modifier.pointerInput(Unit) {
            detectTapGestures(
                onTap = { latestTouch.onSingleTap() },
                onDoubleTap = { latestTouch.onDoubleTap() },
                onLongPress = { latestLongPress?.invoke(it) }
            )
        }
    }
