package com.auradock.dock

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput

/**
 * What touching the dock does, the same in the app and in the screensaver: a single tap only shows "Undo focus" (when Focus is on), a
 * double tap leaves the dock. Swipe (change widget) and long press (edit) belong to the Split template.
 */
data class DockTouch(val onSingleTap: () -> Unit = {}, val onDoubleTap: () -> Unit = {})

/** Applies the dock's tap rules; with [touch] null (previews, thumbnails, the gallery) it does nothing and takes no touches. */
fun Modifier.dockTouch(touch: DockTouch?, onLongPress: ((Offset) -> Unit)? = null): Modifier =
    if (touch == null) this else pointerInput(touch, onLongPress) {
        detectTapGestures(onTap = { touch.onSingleTap() }, onDoubleTap = { touch.onDoubleTap() }, onLongPress = onLongPress)
    }
