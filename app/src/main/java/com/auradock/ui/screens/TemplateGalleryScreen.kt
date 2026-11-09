package com.auradock.ui.screens

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.util.lerp
import com.auradock.dock.DockContent
import com.auradock.dock.DockTemplate
import com.auradock.dock.DockUiState
import com.auradock.dock.world.WorldCity
import com.auradock.ui.components.AuraSheet
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.auradock.ui.components.Badge
import com.auradock.ui.components.PagerDots
import com.auradock.ui.components.PrimaryPill
import com.auradock.ui.components.ScaledPreview
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import com.auradock.ui.theme.AuraDockBold
import com.auradock.ui.theme.BoldPalette
import com.auradock.ui.theme.AuraDockColors
import com.auradock.ui.theme.AuraDockGallery
import com.auradock.ui.theme.AuraDockRadii
import com.auradock.ui.theme.AuraDockSizes
import com.auradock.ui.theme.AuraDockSpacing
import com.auradock.ui.theme.AuraDockTypography
import com.auradock.ui.theme.fromScreenBottom
import com.auradock.ui.theme.fromScreenTop
import kotlin.math.abs

/** What the gallery shows: which template is in use, the data every thumbnail is drawn with, and the templates to pick from. */
data class TemplateGalleryUiState(
    val selected: DockTemplate,
    val dockState: DockUiState,
    val templates: List<DockTemplate> = DockTemplate.available
)

/**
 * Pick one of the available dock templates (handoff Templates and Pick-your-dock frames). Stateless apart from which page the
 * pager is on. With [footer] set it is the last step of the walkthrough: a title and a line instead of a back arrow, the progress
 * dots, "Default" instead of "In use", and a button that always applies.
 */
@Composable
@OptIn(ExperimentalFoundationApi::class)
fun TemplateGalleryScreen(
    state: TemplateGalleryUiState,
    onApply: (DockTemplate) -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Templates",
    /** Shown above the button (the walkthrough's progress dots). When set, the button always applies, even to the template in use. */
    footer: (@Composable () -> Unit)? = null,
    /** Tapping a colour dot under the Bold card (the gallery only; null hides the row). The card previews it at once. */
    onPickPalette: ((BoldPalette) -> Unit)? = null,
    /** The World dock's extra cities (the gallery only; null hides the row). */
    onPickCities: ((List<WorldCity>) -> Unit)? = null
) {
    val selected = state.selected
    val templates = state.templates
    val walkthrough = footer != null
    val pagerState = rememberPagerState(initialPage = templates.indexOf(selected).coerceAtLeast(0), pageCount = { templates.size })
    val shown = templates[pagerState.currentPage]
    val haptic = LocalHapticFeedback.current
    val back = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    var citiesOpen by remember { mutableStateOf(false) }
    Box(modifier = modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AuraDockColors.Background)
            .padding(top = if (walkthrough) fromScreenTop(AuraDockSpacing.huge) else AuraDockSpacing.sm, bottom = fromScreenBottom(AuraDockSpacing.xxl - AuraDockSpacing.sm))
    ) {
        if (!walkthrough) {
            Box(
                modifier = Modifier
                    .padding(start = AuraDockSpacing.smd)
                    .size(AuraDockSizes.touchTarget)
                    .clickable(role = Role.Button) { back?.onBackPressed() },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = AuraDockColors.OnBackground, modifier = Modifier.size(AuraDockSizes.icon)) }
        }
        Column(modifier = Modifier.padding(horizontal = AuraDockSpacing.xl - AuraDockSpacing.xs).padding(top = if (walkthrough) AuraDockSizes.none else AuraDockSpacing.sm)) {
            Text(text = title, color = AuraDockColors.OnBackground, style = AuraDockTypography.StepTitle)
            if (walkthrough) {
                Text(
                    text = "Swipe to preview. Change it any time.",
                    color = AuraDockColors.OnSurfaceVariant,
                    style = AuraDockTypography.Secondary,
                    modifier = Modifier.padding(top = AuraDockSpacing.smd)
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AuraDockSpacing.xl - AuraDockSpacing.xs, Alignment.CenterVertically)
        ) {
            TemplateCarousel(pagerState, templates, selected, state.dockState)
            if (onPickCities != null && shown == DockTemplate.WORLD) {
                WorldCitiesRow(
                    cities = state.dockState.world.extras,
                    onClick = { citiesOpen = true },
                    modifier = Modifier.padding(horizontal = AuraDockGallery.textInset)
                )
            }
            if (onPickPalette != null && DockTemplate.BOLD in templates) {
                PaletteDots(
                    current = state.dockState.boldPalette,
                    visible = shown == DockTemplate.BOLD,
                    onPick = onPickPalette,
                    modifier = Modifier.padding(horizontal = AuraDockGallery.textInset - AuraDockBold.PaletteDotRoom)
                )
            }
            Column(
                modifier = Modifier.padding(horizontal = AuraDockGallery.textInset),
                verticalArrangement = Arrangement.spacedBy(AuraDockSpacing.xs + AuraDockSpacing.xxs)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AuraDockSpacing.smd - AuraDockSpacing.xxs)) {
                    Text(text = shown.label, color = AuraDockColors.OnBackground, style = AuraDockTypography.TemplateName)
                    if (walkthrough) {
                        if (shown == DockTemplate.default) Badge("Default")
                    } else if (shown == selected) Badge("In use", checked = true)
                }
                Text(text = shown.description, color = AuraDockColors.OnSurfaceVariant, style = AuraDockTypography.Secondary)
                if (walkthrough) {
                    Text(
                        text = "${pagerState.currentPage + 1} of ${templates.size} · ${templates.joinToString { it.label }}",
                        color = AuraDockColors.Tertiary,
                        style = AuraDockTypography.RowCaption,
                        modifier = Modifier.padding(top = AuraDockSpacing.smd - AuraDockSpacing.xxs)
                    )
                }
            }
            if (!walkthrough) {
                PagerDots(count = templates.size, current = pagerState.currentPage, modifier = Modifier.align(Alignment.CenterHorizontally))
            }
        }

        Column(
            modifier = Modifier.padding(horizontal = AuraDockSpacing.xl - AuraDockSpacing.xs),
            verticalArrangement = Arrangement.spacedBy(if (walkthrough) AuraDockSpacing.xl - AuraDockSpacing.xs else AuraDockSpacing.md)
        ) {
            if (walkthrough) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { footer?.invoke() }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(AuraDockRadii.row))
                        .background(AuraDockColors.Surface)
                        .padding(horizontal = AuraDockSpacing.mdl - AuraDockSpacing.xxs, vertical = AuraDockSpacing.xs)
                ) {
                    FactRow("Shows", shown.shows, divider = false)
                    FactRow("Best for", shown.bestFor, divider = true)
                }
            }
            PrimaryPill(
                text = "Use this dock",
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onApply(shown)
                },
                enabled = true
            )
        }
    }
    if (citiesOpen && onPickCities != null) {
        AuraSheet(onDismiss = { citiesOpen = false }) {
            WorldCitiesSheetContent(selected = state.dockState.world.extras, onChange = onPickCities)
        }
    }
    }
}

/** One line of the facts card: a grey label on the left, the value on the right. */
@Composable
private fun FactRow(label: String, value: String, divider: Boolean) {
    Column {
        if (divider) Box(Modifier.fillMaxWidth().size(AuraDockSizes.line).background(AuraDockColors.RowDivider))
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = AuraDockSpacing.smd + AuraDockSpacing.xxs),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, color = AuraDockColors.OnSurfaceVariant, style = AuraDockTypography.Secondary)
            Text(text = value, color = AuraDockColors.OnBackground, style = AuraDockTypography.Secondary)
        }
    }
}

/**
 * The row of template cards. The card in the middle sits at full size; its neighbours are smaller and dimmer, pinned to the side
 * nearest the middle, as the handoff draws them.
 */
@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun TemplateCarousel(pagerState: PagerState, templates: List<DockTemplate>, selected: DockTemplate, dockState: DockUiState) {
    HorizontalPager(
        state = pagerState,
        pageSize = PageSize.Fixed(AuraDockGallery.cardWidth),
        contentPadding = PaddingValues(start = AuraDockGallery.textInset, end = AuraDockGallery.textInset),
        pageSpacing = AuraDockGallery.cardGap,
        modifier = Modifier.fillMaxWidth()
    ) { page ->
        val offset = (page - pagerState.currentPage) - pagerState.currentPageOffsetFraction
        val distance = abs(offset).coerceIn(0f, 1f)
        val scale = lerp(1f, AuraDockGallery.NeighbourScale, distance)
        TemplateCard(
            template = templates[page],
            state = dockState,
            inUse = templates[page] == selected,
            modifier = Modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = lerp(1f, AuraDockGallery.NeighbourAlpha, distance)
                transformOrigin = TransformOrigin(if (offset > 0f) 0f else 1f, 0.5f)
            },
            current = distance < 0.5f
        )
    }
}

@Composable
private fun TemplateCard(template: DockTemplate, state: DockUiState, inUse: Boolean, modifier: Modifier = Modifier, current: Boolean) {
    val shape = RoundedCornerShape(AuraDockRadii.templateCard)
    Box(
        modifier = modifier
            .size(AuraDockGallery.cardWidth, AuraDockGallery.cardHeight)
            .clip(shape)
            .background(AuraDockColors.Background)
            .border(if (current) AuraDockSizes.lineThick else AuraDockSizes.line, if (current) AuraDockColors.Accent else AuraDockColors.Field, shape)
    ) {
        // The real dock for this template, shrunk to the card.
        ScaledPreview(designWidth = AuraDockGallery.previewDesignWidth, modifier = Modifier.fillMaxSize()) {
            DockContent(template = template, state = state)
        }
    }
}

/** The Bold card's colourways as a row of dots; the one in use has a ring. Kept in place (just hidden) on the other cards so nothing jumps. */
@Composable
private fun PaletteDots(current: BoldPalette, visible: Boolean, onPick: (BoldPalette) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier.alpha(if (visible) 1f else 0f), verticalAlignment = Alignment.CenterVertically) {
        BoldPalette.entries.forEach { palette ->
            val chosen = palette == current
            Box(
                modifier = Modifier
                    .size(AuraDockBold.PaletteHit)
                    .clickable(enabled = visible, role = Role.RadioButton, onClickLabel = palette.label) { onPick(palette) }
                    .semantics { contentDescription = "${palette.label} colour"; selected = chosen },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(AuraDockBold.PaletteDot)
                        .clip(CircleShape)
                        .background(palette.tone(0))
                        .border(if (chosen) AuraDockSizes.lineThick else AuraDockSizes.none, AuraDockColors.OnBackground, CircleShape)
                )
            }
        }
    }
}
