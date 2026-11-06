package com.auradock.ui.screens

import com.auradock.ui.theme.AuraDockTypography
import com.auradock.ui.theme.AuraDockRadii
import com.auradock.ui.theme.AuraDockGallery
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.auradock.dock.DockContent
import com.auradock.dock.DockTemplate
import com.auradock.dock.DockUiState
import com.auradock.ui.components.ScaledPreview
import com.auradock.ui.components.Badge
import com.auradock.ui.components.PrimaryPill
import com.auradock.ui.theme.AuraDockColors
import com.auradock.ui.theme.AuraDockSpacing
import kotlin.math.abs

/** What the gallery shows: which template is in use, the data every thumbnail is drawn with, and the templates to pick from. */
data class TemplateGalleryUiState(
    val selected: DockTemplate,
    val dockState: DockUiState,
    val templates: List<DockTemplate> = DockTemplate.available
)

/** Pick one of the available dock templates. Stateless apart from which page the pager is on. */
@Composable
@OptIn(ExperimentalFoundationApi::class)
fun TemplateGalleryScreen(
    state: TemplateGalleryUiState,
    onApply: (DockTemplate) -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Templates",
    /** Shown above the button (the walkthrough's progress dots). When set, the button always applies, even to the template in use. */
    footer: (@Composable () -> Unit)? = null
) {
    val selected = state.selected
    val templates = state.templates
    val pagerState = rememberPagerState(
        initialPage = templates.indexOf(selected).coerceAtLeast(0),
        pageCount = { templates.size }
    )
    val shownIndex = pagerState.currentPage
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AuraDockColors.Background)
            .padding(horizontal = AuraDockSpacing.mdl, vertical = AuraDockSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(AuraDockSpacing.lg)
    ) {
        Text(
            text = title,
            color = AuraDockColors.OnBackground,
            style = AuraDockTypography.ScreenTitle
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            HorizontalPager(
                state = pagerState,
                contentPadding = PaddingValues(horizontal = AuraDockGallery.pageInset),
                pageSpacing = AuraDockSpacing.md,
                modifier = Modifier.fillMaxWidth()
            ) { page ->
                val pageOffset = (
                    (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                ).let { abs(it) }.coerceIn(0f, 1f)
                val scale = androidx.compose.ui.util.lerp(0.85f, 1f, 1f - pageOffset)
                val alpha = androidx.compose.ui.util.lerp(0.5f, 1f, 1f - pageOffset)

                TemplateCard(
                    template = templates[page],
                    state = state.dockState,
                    inUse = templates[page] == selected,
                    modifier = Modifier.graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
                )
            }
        }

        val shown = templates[shownIndex]
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AuraDockSpacing.md)
        ) {
            footer?.invoke()
            PrimaryPill(
                text = if (shown == selected && footer == null) "In use" else if (footer != null) "Continue with ${shown.label}" else "Use ${shown.label}",
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onApply(shown)
                },
                enabled = footer != null || shown != selected
            )
        }
    }
}

@Composable
private fun TemplateCard(
    template: DockTemplate,
    state: DockUiState,
    inUse: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AuraDockRadii.card))
            .background(AuraDockColors.Surface)
            .padding(AuraDockGallery.cardPadding),
        verticalArrangement = Arrangement.spacedBy(AuraDockSpacing.smd)
    ) {
        // The real dock for this template, shrunk to a landscape thumbnail.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(AuraDockRadii.control))
        ) {
            ScaledPreview(designWidth = AuraDockGallery.previewDesignWidth) {
                DockContent(template = template, state = state)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AuraDockSpacing.sm)) {
            Text(text = template.label, color = AuraDockColors.OnBackground, style = AuraDockTypography.Section)
            if (inUse) Badge("In use")
        }
        Text(text = template.description, color = AuraDockColors.OnSurfaceVariant, style = AuraDockTypography.Secondary)
    }
}
