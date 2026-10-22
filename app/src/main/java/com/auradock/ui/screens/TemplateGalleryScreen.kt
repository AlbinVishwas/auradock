package com.auradock.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auradock.ui.components.GlassCard
import com.auradock.ui.components.QuickActionButton
import com.auradock.ui.template.StandbyTemplate
import com.auradock.ui.template.TemplateDefaults
import com.auradock.ui.theme.AuraDockSpacing
import com.auradock.ui.theme.AuraDockTheme
import kotlin.math.abs

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun TemplateGalleryScreen(
    modifier: Modifier = Modifier,
    templates: List<StandbyTemplate> = TemplateDefaults.gallery,
    onPreviewClick: (StandbyTemplate) -> Unit = {},
    onApplyClick: (StandbyTemplate) -> Unit = {}
) {
    val templateItems = templates.ifEmpty { listOf(TemplateDefaults.Minimal) }
    val pagerState = rememberPagerState(pageCount = { templateItems.size })
    val selectedIndex = pagerState.currentPage
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = AuraDockSpacing.lg, vertical = AuraDockSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(AuraDockSpacing.lg)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AuraDockSpacing.xs)) {
            Text(
                text = "Choose Your Standby",
                fontSize = 22.sp,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Template-driven and AI-ready",
                fontSize = 14.sp,
                color = Color.Gray
            )
        }

        Box(
            modifier = Modifier.weight(1f),
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
                ).absoluteValue.coerceIn(0f, 1f)
                val scale = androidx.compose.ui.util.lerp(0.85f, 1f, 1f - pageOffset)
                val alpha = androidx.compose.ui.util.lerp(0.5f, 1f, 1f - pageOffset)

                TemplateCard(
                    template = templateItems[page],
                    selected = page == selectedIndex,
                    modifier = Modifier.graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AuraDockSpacing.md)
        ) {
            QuickActionButton(
                text = "Preview",
                onClick = { onPreviewClick(templateItems[selectedIndex]) },
                modifier = Modifier.weight(1f)
            )
            QuickActionButton(
                text = "Apply",
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onApplyClick(templateItems[selectedIndex])
                },
                modifier = Modifier.weight(1f),
                isActive = true
            )
        }
    }
}

@Composable
private fun TemplateCard(
    template: StandbyTemplate,
    selected: Boolean,
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
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF111111)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(template.style.accentColor.copy(alpha = if (selected) 0.22f else 0.14f))
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = template.layoutType.name,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Light
                    )
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth(0.42f)
                            .height(1.dp)
                            .background(Color.White.copy(alpha = 0.14f))
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .width(48.dp)
                                .height(34.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                        )
                        Box(
                            modifier = Modifier
                                .width(48.dp)
                                .height(34.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                        )
                    }
                }
            }

            Text(
                text = template.name,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
            )
        }
    }
}

private val Float.absoluteValue: Float
    get() = abs(this)

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun TemplateGalleryScreenPreview() {
    AuraDockTheme {
        TemplateGalleryScreen()
    }
}
