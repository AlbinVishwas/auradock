package com.auradock.watch

import androidx.compose.runtime.Composable
import com.auradock.dock.Pill
import com.auradock.ui.preview.PreviewComponent
import com.auradock.ui.theme.AuraDockTheme

@PreviewComponent
@Composable
private fun PillCountdown() = AuraDockTheme { PillContent(Pill.Countdown(5), visible = true, onTap = {}, onCancel = {}) }

@PreviewComponent
@Composable
private fun PillOffer() = AuraDockTheme { PillContent(Pill.Offer, visible = true, onTap = {}, onCancel = {}) }
