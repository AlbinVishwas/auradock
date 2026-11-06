package com.auradock.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import com.auradock.dock.DockTemplate
import com.auradock.ui.components.BlurIn
import com.auradock.ui.components.GreyTextButton
import com.auradock.ui.components.PrimaryPill
import com.auradock.ui.components.ProgressDots
import com.auradock.ui.components.ScaledPreview
import com.auradock.dock.DockContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import com.auradock.R
import com.auradock.ui.theme.AuraDockGallery
import com.auradock.ui.theme.AuraDockRadii
import com.auradock.ui.theme.AuraDockSizes
import com.auradock.ui.focus.DockFromAnywhereCard
import com.auradock.ui.screens.TemplateGalleryScreen
import com.auradock.ui.theme.AuraDockColors
import com.auradock.ui.screens.TemplateGalleryUiState
import com.auradock.ui.theme.AuraDockSpacing
import com.auradock.ui.theme.AuraDockTypography

private enum class Step { WELCOME, SCREENSAVER, DOCK_ANYWHERE, PERMISSIONS, TEMPLATE }

/** What the first-launch walkthrough shows. */
data class OnboardingUiState(
    val granted: Set<SetupPermission>,
    val screensaverSelected: Boolean,
    val dockFromAnywhere: Boolean,
    val overlayGranted: Boolean,
    /** The last step: pick a template. */
    val gallery: TemplateGalleryUiState
)

/** What the user can do in the walkthrough. */
data class OnboardingActions(
    val onAllowPermission: (SetupPermission) -> Unit = {},
    val onOpenScreensaverSettings: () -> Unit = {},
    val onOpenDisplaySettings: () -> Unit = {},
    val onToggleDockFromAnywhere: (Boolean) -> Unit = {},
    val onAllowOverlay: () -> Unit = {},
    val onPickTemplate: (DockTemplate) -> Unit = {},
    /** Runs after the template is picked; the caller saves that the walkthrough is done and shows the dashboard. */
    val onFinish: () -> Unit = {}
)

/**
 * The first-launch walkthrough: Welcome, dock explanation and screensaver, "Dock from anywhere", permissions, pick a template.
 * Only the current step is remembered here, because it is about this screen alone. Progress dots at the bottom, and Continue
 * is the one primary pill.
 */
@Composable
fun OnboardingScreen(
    state: OnboardingUiState,
    actions: OnboardingActions,
    modifier: Modifier = Modifier,
    initialStep: Int = 0
) {
    var stepIndex by rememberSaveable { mutableIntStateOf(initialStep) }
    val step = Step.entries[stepIndex]
    BackHandler(enabled = stepIndex > 0) { stepIndex-- }
    val dots = @Composable { ProgressDots(count = Step.entries.size, current = stepIndex) }

    if (step == Step.TEMPLATE) {
        TemplateGalleryScreen(
            state = state.gallery,
            modifier = modifier,
            title = "Pick your dock",
            footer = dots,
            onApply = {
                actions.onPickTemplate(it)
                actions.onFinish()
            }
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AuraDockColors.Background)
            .padding(horizontal = AuraDockSpacing.mdl, vertical = AuraDockSpacing.lg)
    ) {
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AuraDockSpacing.md)
        ) {
            key(step) {
                BlurIn {
                    Column(verticalArrangement = Arrangement.spacedBy(AuraDockSpacing.md)) { StepContent(step, state, actions) }
                }
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = AuraDockSpacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AuraDockSpacing.sm)
        ) {
            dots()
            PrimaryPill(text = if (step == Step.WELCOME) "Get started" else "Continue", onClick = { stepIndex++ })
            if (step == Step.DOCK_ANYWHERE || step == Step.PERMISSIONS) GreyTextButton(text = "Skip for now", onClick = { stepIndex++ })
        }
    }
}

@Composable
private fun StepContent(step: Step, state: OnboardingUiState, actions: OnboardingActions) {
    when (step) {
        Step.WELCOME -> {
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.size(AuraDockSizes.wordmarkMark)
            )
            Text(text = "AuraDock", color = AuraDockColors.OnBackground, style = AuraDockTypography.ScreenTitle)
            Text(
                text = "Your phone becomes a calm dock while it charges.",
                style = AuraDockTypography.Secondary,
                color = AuraDockColors.OnSurfaceVariant
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(AuraDockRadii.card))
            ) {
                ScaledPreview(designWidth = AuraDockGallery.previewDesignWidth) {
                    DockContent(template = state.gallery.selected, state = state.gallery.dockState)
                }
            }
        }
        Step.SCREENSAVER -> DockExplainer(
            screensaverSelected = state.screensaverSelected,
            onChooseScreensaver = actions.onOpenScreensaverSettings,
            onSetScreenTimeout = actions.onOpenDisplaySettings
        )
        Step.DOCK_ANYWHERE -> {
            Text(text = "Dock from anywhere", color = AuraDockColors.OnBackground, style = AuraDockTypography.ScreenTitle)
            Text(text = "Optional. You can turn it on later in Focus.", style = AuraDockTypography.Secondary, color = AuraDockColors.OnSurfaceVariant)
            DockFromAnywhereCard(state.dockFromAnywhere, state.overlayGranted, actions.onToggleDockFromAnywhere, actions.onAllowOverlay)
        }
        Step.PERMISSIONS -> {
            Text(text = "Permissions", color = AuraDockColors.OnBackground, style = AuraDockTypography.ScreenTitle)
            Text(
                text = "Allow what you want. Everything is optional; allow it later in Setup.",
                style = AuraDockTypography.Secondary,
                color = AuraDockColors.OnSurfaceVariant
            )
            SetupPermission.entries.forEach { permission ->
                PermissionCard(
                    permission = permission,
                    granted = permission in state.granted,
                    onAllow = { actions.onAllowPermission(permission) }
                )
            }
        }
        Step.TEMPLATE -> Unit
    }
}
