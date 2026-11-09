package com.auradock.ui.debug

import androidx.compose.runtime.Composable
import com.auradock.analytics.ActivationSummary
import com.auradock.ui.preview.PreviewPortrait
import com.auradock.ui.theme.AuraDockTheme

private val Sample = ActivationSummary(
    activatedWithin24h = true,
    screensaverSetAt = 1_794_000_000_000,
    firstDockAt = 1_794_003_600_000,
    dockedDays = 5,
    dockedFiveOfSeven = true,
    counts = listOf("session_start" to 9, "task_added" to 6, "task_done" to 4, "first_dock" to 1),
    total = 20
)

@PreviewPortrait
@Composable
private fun DebugActivated() = AuraDockTheme { DebugScreen(Sample, version = "0.2.2") }

@PreviewPortrait
@Composable
private fun DebugNothingYet() = AuraDockTheme {
    DebugScreen(ActivationSummary(false, null, null, 0, false, emptyList(), 0), version = "0.2.2")
}

@PreviewPortrait
@Composable
private fun DebugLoading() = AuraDockTheme { DebugScreen(null, version = "0.2.2") }
