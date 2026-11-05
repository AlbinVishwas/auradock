package com.auradock.ui.screens

import androidx.compose.runtime.Composable
import com.auradock.dock.DockTemplate
import com.auradock.ui.preview.PreviewData
import com.auradock.ui.preview.PreviewPortrait
import com.auradock.ui.theme.AuraDockTheme

@PreviewPortrait
@Composable
private fun GalleryFull() = AuraDockTheme {
    TemplateGalleryScreen(TemplateGalleryUiState(DockTemplate.CLASSIC, PreviewData.dockFull), onApply = {})
}

/** With nothing to show, every thumbnail is just a clock. */
@PreviewPortrait
@Composable
private fun GalleryEmpty() = AuraDockTheme {
    TemplateGalleryScreen(TemplateGalleryUiState(DockTemplate.FOCUS, PreviewData.dockEmpty), onApply = {})
}
