package com.auradock.ui.theme

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp

/*
 * The app draws edge to edge (Android 15 and later force it, and MainActivity asks for it on older phones too): the window reaches the top and
 * bottom of the screen and the status and navigation bars sit on top of it. The handoff measures its frames from the screen's edges, so a
 * design distance is used as it is, but never less than what keeps the content clear of the bars. The insets are read from Compose, so they
 * follow rotation, split screen and the bars coming and going.
 */

/** A distance the handoff measures from the top of the screen, as padding from the window's top edge (clear of the status bar). */
@Composable
fun fromScreenTop(designTop: Dp): Dp {
    val bar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    return maxOf(designTop, bar + AuraDockSpacing.smd)
}

/** A distance the handoff measures from the bottom of the screen, as padding from the window's bottom edge (clear of the navigation bar). */
@Composable
fun fromScreenBottom(designBottom: Dp): Dp {
    val bar = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    return maxOf(designBottom, bar + AuraDockSpacing.md)
}
