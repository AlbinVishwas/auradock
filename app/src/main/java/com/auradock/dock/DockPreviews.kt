package com.auradock.dock

import androidx.compose.runtime.Composable
import com.auradock.ui.preview.PreviewData
import com.auradock.ui.preview.PreviewLandscape
import com.auradock.ui.preview.PreviewPortrait
import com.auradock.ui.theme.AuraDockTheme

// The dock is landscape-only, so every template has landscape previews. A portrait one is added too, to see how a template
// holds up if the window is ever the wrong way round (the dock normally rotates its content, see RotatedContent).

@PreviewLandscape @Composable private fun ClassicFull() = AuraDockTheme { DockContent(DockTemplate.CLASSIC, PreviewData.dockFull) }
@PreviewLandscape @Composable private fun ClassicEmpty() = AuraDockTheme { DockContent(DockTemplate.CLASSIC, PreviewData.dockEmpty) }

@PreviewLandscape @Composable private fun FocusFull() = AuraDockTheme { DockContent(DockTemplate.FOCUS, PreviewData.dockFull) }
@PreviewLandscape @Composable private fun FocusEmpty() = AuraDockTheme { DockContent(DockTemplate.FOCUS, PreviewData.dockEmpty) }

@PreviewLandscape @Composable private fun MinimalFull() = AuraDockTheme { DockContent(DockTemplate.MINIMAL, PreviewData.dockFull) }
@PreviewLandscape @Composable private fun MinimalEmpty() = AuraDockTheme { DockContent(DockTemplate.MINIMAL, PreviewData.dockEmpty) }

@PreviewLandscape @Composable private fun ProductivityFull() = AuraDockTheme { DockContent(DockTemplate.PRODUCTIVITY, PreviewData.dockFull) }

/** Many tasks and events, to see "+N more". */
@PreviewLandscape
@Composable
private fun ProductivityBusy() = AuraDockTheme {
    DockContent(
        DockTemplate.PRODUCTIVITY,
        PreviewData.dockFull.copy(tasks = PreviewData.tasks + PreviewData.tasks.map { it.copy(id = it.id + 10, done = false) })
    )
}

@PreviewLandscape @Composable private fun ProductivityEmpty() = AuraDockTheme { DockContent(DockTemplate.PRODUCTIVITY, PreviewData.dockEmpty) }

@PreviewPortrait @Composable private fun ClassicPortrait() = AuraDockTheme { DockContent(DockTemplate.CLASSIC, PreviewData.dockFull) }
@PreviewPortrait @Composable private fun ProductivityPortrait() = AuraDockTheme { DockContent(DockTemplate.PRODUCTIVITY, PreviewData.dockFull) }
