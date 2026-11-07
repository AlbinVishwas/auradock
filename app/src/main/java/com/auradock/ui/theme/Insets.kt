package com.auradock.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/** The real heights of the status bar (top) and the navigation bar (bottom), read from the window. The app draws between them. */
@Composable
private fun systemBars(): Pair<Dp, Dp> {
    val view = LocalView.current
    val density = LocalDensity.current
    return remember(view, density) {
        val bars = ViewCompat.getRootWindowInsets(view)?.getInsets(WindowInsetsCompat.Type.systemBars())
        with(density) { (bars?.top ?: 0).toDp() to (bars?.bottom ?: 0).toDp() }
    }
}

/** A distance the handoff measures from the top of the screen, as padding below the status bar. */
@Composable
fun fromScreenTop(designTop: Dp): Dp = (designTop - systemBars().first).coerceAtLeast(AuraDockSpacing.smd)

/** A distance the handoff measures from the bottom of the screen, as padding above the navigation bar. */
@Composable
fun fromScreenBottom(designBottom: Dp): Dp = (designBottom - systemBars().second).coerceAtLeast(AuraDockSpacing.md)
