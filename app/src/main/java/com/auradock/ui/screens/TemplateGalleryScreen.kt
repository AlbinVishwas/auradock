package com.auradock.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auradock.dock.DockContent
import com.auradock.dock.DockTemplate
import com.auradock.dock.DockUiState
import com.auradock.ui.components.ScaledPreview
import com.auradock.ui.components.GlassCard
import com.auradock.ui.components.QuickActionButton
import com.auradock.ui.theme.AuraDockColors
import com.auradock.ui.theme.AuraDockSpacing
import com.auradock.ui.theme.AuraDockTheme
import kotlin.math.abs

/** Pick one of the available dock templates. [selected] is the one currently in use. */
@Composable
@OptIn(ExperimentalFoundationApi::class)
fun TemplateGalleryScreen(
    selected: DockTemplate,
    state: DockUiState,
    modifier: Modifier = Modifier,
    templates: List<DockTemplate> = DockTemplate.available,
    onApplyClick: (DockTemplate) -> Unit = {}
) {
    val pagerState = rememberPagerState(
        initialPage = templates.indexOf(selected).coerceAtLeast(0),
        pageCount = { templates.size }
    )
    val shownIndex = pagerState.currentPage
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = AuraDockSpacing.lg, vertical = AuraDockSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(AuraDockSpacing.lg)
    ) {
        Text(
            text = "Templates",
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.SemiBold
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            HorizontalPager(
                state = pagerState,
                contentPadding = PaddingValues(horizontal = 64.dp),
                pageSpacing = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) { page ->
                val pageOffset = (
                    (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                ).let { abs(it) }.coerceIn(0f, 1f)
                val scale = androidx.compose.ui.util.lerp(0.85f, 1f, 1f - pageOffset)
                val alpha = androidx.compose.ui.util.lerp(0.5f, 1f, 1f - pageOffset)

                TemplateCard(
                    template = templates[page],
                    state = state,
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
        QuickActionButton(
            text = if (shown == selected) "In use" else "Use ${shown.label}",
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onApplyClick(shown)
            },
            modifier = Modifier.fillMaxWidth(),
            isActive = shown != selected
        )
    }
}

@Composable
private fun TemplateCard(
    template: DockTemplate,
    state: DockUiState,
    inUse: Boolean,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.72f),
        cornerRadius = 24.dp,
        padding = 18.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(AuraDockSpacing.md)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                // The real dock for this template, shrunk to a thumbnail.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    ScaledPreview(designWidth = 800.dp) {
                        DockContent(template = template, state = state)
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(AuraDockSpacing.sm)) {
                Text(
                    text = template.label,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = if (inUse) FontWeight.SemiBold else FontWeight.Medium
                )
                if (inUse) {
                    Text(text = "· in use", color = AuraDockColors.ClockGlow, fontSize = 14.sp)
                }
            }
            Text(
                text = template.description,
                color = AuraDockColors.OnSurfaceVariant,
                fontSize = 14.sp
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun TemplateGalleryScreenPreview() {
    AuraDockTheme {
        TemplateGalleryScreen(selected = DockTemplate.CLASSIC, state = DockUiState(System.currentTimeMillis()))
    }
}
