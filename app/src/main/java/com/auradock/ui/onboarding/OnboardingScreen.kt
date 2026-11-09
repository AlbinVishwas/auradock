package com.auradock.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.DesktopWindows
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay
import com.auradock.ui.focus.DockFromAnywhereCard
import com.auradock.ui.theme.AuraDockMotion
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.sp
import com.auradock.dock.DockContent
import com.auradock.dock.DockTemplate
import com.auradock.ui.components.BlurIn
import com.auradock.ui.components.GreyTextButton
import com.auradock.ui.components.PrimaryPill
import com.auradock.ui.components.ProgressDots
import com.auradock.ui.screens.TemplateGalleryScreen
import com.auradock.ui.screens.TemplateGalleryUiState
import com.auradock.ui.theme.AuraDockColors
import com.auradock.ui.theme.AuraDockGallery
import com.auradock.ui.theme.AuraDockIllustration
import com.auradock.ui.theme.AuraDockRadii
import com.auradock.ui.theme.AuraDockSizes
import com.auradock.ui.theme.AuraDockSpacing
import com.auradock.ui.theme.AuraDockTypography
import com.auradock.ui.theme.fromScreenBottom
import com.auradock.ui.theme.fromScreenTop

/**
 * The first run: Welcome, then the dock itself (a five-second full-screen preview), then the template, then the two optional steps
 * about opening on its own. No permission is asked here: each is asked when its feature is first used (see PermissionSheet).
 */
private enum class Step { WELCOME, PREVIEW, TEMPLATE, SCREENSAVER, ANYWHERE }

/** What the first-launch walkthrough shows. */
data class OnboardingUiState(
    val granted: Set<SetupPermission>,
    val screensaverSelected: Boolean,
    val dockFromAnywhere: Boolean,
    val overlayGranted: Boolean,
    /** The template step, and the dock the preview shows. */
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
    /** The dock preview came on screen (true) or went away (false): the window turns landscape while it shows. */
    val onPreviewing: (Boolean) -> Unit = {},
    /** Runs after the last step; the caller saves that the walkthrough is done and shows the dashboard. */
    val onFinish: () -> Unit = {}
)

/**
 * The first-launch walkthrough. Only the current step is remembered here, because it is about this screen alone. Progress dots at
 * the bottom, and one primary pill; the preview fills the screen and moves on by itself.
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
    // Back from the template or the preview goes to Welcome (the preview is not replayed).
    BackHandler(enabled = stepIndex > 0) { stepIndex = if (step == Step.TEMPLATE || step == Step.PREVIEW) 0 else stepIndex - 1 }
    val dots = @Composable { ProgressDots(count = Step.entries.size, current = stepIndex) }

    if (step == Step.PREVIEW) {
        DockPreview(state, actions, onDone = { stepIndex++ }, modifier = modifier)
        return
    }
    if (step == Step.TEMPLATE) {
        TemplateGalleryScreen(
            state = state.gallery,
            modifier = modifier,
            title = "Pick your dock",
            footer = dots,
            onApply = {
                actions.onPickTemplate(it)
                stepIndex++
            }
        )
        return
    }

    val horizontal = AuraDockSpacing.xl - AuraDockSpacing.xs
    val top = when (step) {
        Step.WELCOME -> AuraDockSpacing.huge + AuraDockSpacing.huge + AuraDockSpacing.sm
        Step.SCREENSAVER -> AuraDockSpacing.huge + AuraDockSpacing.sm
        else -> AuraDockSpacing.huge
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AuraDockColors.Background)
            .padding(start = horizontal, end = horizontal, top = fromScreenTop(top), bottom = fromScreenBottom(AuraDockSpacing.xxl - AuraDockSpacing.sm))
    ) {
        Column(modifier = Modifier.weight(1f).then(if (step == Step.ANYWHERE) Modifier.verticalScroll(rememberScrollState()) else Modifier)) {
            key(step) { BlurIn { StepContent(step, state, actions) } }
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = AuraDockSpacing.md),
            verticalArrangement = Arrangement.spacedBy(if (step == Step.SCREENSAVER) AuraDockSpacing.smd else AuraDockSpacing.xl - AuraDockSpacing.xs)
        ) {
            Box(Modifier.fillMaxWidth().padding(bottom = if (step == Step.SCREENSAVER) AuraDockSpacing.md else AuraDockSizes.none), contentAlignment = Alignment.Center) { dots() }
            when (step) {
                Step.WELCOME -> PrimaryPill(text = "Get started", onClick = { stepIndex++ })
                Step.SCREENSAVER -> {
                    PrimaryPill(text = "Open screensaver settings", onClick = { actions.onOpenScreensaverSettings(); stepIndex++ })
                    GreyTextButton(text = "Skip for now", onClick = { stepIndex++ }, modifier = Modifier.fillMaxWidth())
                }
                Step.ANYWHERE -> {
                    PrimaryPill(text = "Done", onClick = actions.onFinish)
                }
                else -> Unit
            }
        }
    }
}

/**
 * "This is AuraDock": the Bold dock, full screen, for about five seconds (or until it is touched). The window turns landscape while
 * it shows, as the real dock does.
 */
@Composable
private fun DockPreview(state: OnboardingUiState, actions: OnboardingActions, onDone: () -> Unit, modifier: Modifier) {
    DisposableEffect(Unit) {
        actions.onPreviewing(true)
        onDispose { actions.onPreviewing(false) }
    }
    LaunchedEffect(Unit) {
        delay(AuraDockMotion.OnboardingPreview.toLong())
        onDone()
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AuraDockColors.Background)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDone)
    ) {
        DockContent(template = DockTemplate.BOLD, state = state.gallery.dockState)
        BlurIn {
            Text(
                text = "This is AuraDock",
                color = AuraDockColors.OnSurfaceVariant,
                style = AuraDockTypography.Secondary,
                modifier = Modifier.align(Alignment.TopStart).padding(start = AuraDockSpacing.xl, top = AuraDockSpacing.xl)
            )
        }
    }
}

@Composable
private fun StepContent(step: Step, state: OnboardingUiState, actions: OnboardingActions) {
    when (step) {
        Step.WELCOME -> Welcome(state)
        Step.SCREENSAVER -> Screensaver()
        Step.ANYWHERE -> DockFromAnywhereStep(state, actions)
        Step.PREVIEW, Step.TEMPLATE -> Unit
    }
}

/** The optional pill that starts the dock from any app (it was in the first walkthrough and is back after the screensaver step). */
@Composable
private fun DockFromAnywhereStep(state: OnboardingUiState, actions: OnboardingActions) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AuraDockSpacing.md)) {
        Text(text = "Start it from anywhere", color = AuraDockColors.OnBackground, style = AuraDockTypography.StepTitle)
        Text(
            text = "Optional. You can turn this on later in Focus settings.",
            color = AuraDockColors.OnSurfaceVariant,
            style = AuraDockTypography.StepBody
        )
        DockFromAnywhereCard(
            enabled = state.dockFromAnywhere,
            overlayGranted = state.overlayGranted,
            onToggle = actions.onToggleDockFromAnywhere,
            onAllowOverlay = actions.onAllowOverlay
        )
    }
}

@Composable
private fun Welcome(state: OnboardingUiState) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(text = "AuraDock", color = AuraDockColors.OnBackground, style = AuraDockTypography.WelcomeTitle)
        Text(
            text = "Your phone, calm and useful on the desk",
            color = AuraDockColors.OnSurfaceVariant,
            style = AuraDockTypography.WelcomeSubtitle,
            modifier = Modifier.padding(top = AuraDockSpacing.md).widthIn(max = AuraDockGallery.welcomeSubtitleWidth)
        )
        // The desk: a small screen on a stand, showing the dock, centred in the space that is left.
        Column(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            val device = RoundedCornerShape(AuraDockRadii.device)
            Box(
                modifier = Modifier
                    .size(AuraDockGallery.deviceWidth, AuraDockGallery.deviceHeight)
                    .clip(device)
                    .border(AuraDockGallery.deviceBorder, AuraDockColors.SurfaceRaised, device)
                    .background(AuraDockColors.Background)
                    .padding(AuraDockGallery.deviceBorder)
            ) { DockContent(template = DockTemplate.BOLD, state = state.gallery.dockState) }
            Box(
                Modifier
                    .size(AuraDockGallery.deviceNeckWidth, AuraDockGallery.deviceNeck)
                    .clip(RoundedCornerShape(bottomStart = AuraDockRadii.badge, bottomEnd = AuraDockRadii.badge))
                    .background(AuraDockColors.SurfaceRaised)
            )
            Box(
                Modifier
                    .padding(top = AuraDockSpacing.xxs)
                    .size(AuraDockGallery.deviceBaseWidth, AuraDockGallery.deviceBaseHeight)
                    .clip(RoundedCornerShape(AuraDockSpacing.xs))
                    .background(AuraDockColors.Surface)
            )
        }
    }
}

@Composable
private fun Screensaver() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().padding(top = AuraDockSpacing.sm, bottom = AuraDockSpacing.xl), contentAlignment = Alignment.Center) { ScreensaverIllustration() }
        Text(text = "Opens on its own", color = AuraDockColors.OnBackground, style = AuraDockTypography.ScreenTitle)
        Text(
            text = "Set AuraDock as your screensaver. It starts by itself when all three are true:",
            color = AuraDockColors.OnSurfaceVariant,
            style = AuraDockTypography.StepBody,
            modifier = Modifier.padding(top = AuraDockSpacing.smd + AuraDockSpacing.xxs)
        )
        Column(modifier = Modifier.padding(top = AuraDockSpacing.lg), verticalArrangement = Arrangement.spacedBy(AuraDockSpacing.smd - AuraDockSpacing.xxs)) {
            Fact(Icons.Outlined.Bolt, "Phone is charging")
            Fact(Icons.Outlined.DesktopWindows, "Sideways on a stand")
            Fact(Icons.Outlined.Schedule, "Screen times out")
        }
    }
}

@Composable
private fun Fact(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AuraDockRadii.row))
            .background(AuraDockColors.Surface)
            .padding(horizontal = AuraDockSpacing.md, vertical = AuraDockSpacing.smd + AuraDockSpacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AuraDockSpacing.smd + AuraDockSpacing.xxs)
    ) {
        Icon(icon, contentDescription = null, tint = AuraDockColors.OnBackground, modifier = Modifier.size(AuraDockSizes.factIcon))
        Text(text = text, color = AuraDockColors.OnBackground, style = AuraDockTypography.Body)
    }
}

/** A phone resting sideways on a stand, plugged in, showing the dock clock (drawn at the handoff's 320 x 210). */
@Composable
private fun ScreensaverIllustration() {
    val measurer = rememberTextMeasurer()
    val style = AuraDockTypography.IllustrationBoldClock.copy(color = AuraDockColors.Coral.copy(alpha = AuraDockIllustration.ClockAlpha))
    Canvas(Modifier.size(AuraDockIllustration.sceneWidth, AuraDockIllustration.sceneHeight)) {
        val u = size.width / 320f
        fun p(v: Float) = v * u
        val line = Stroke(width = p(3f), cap = StrokeCap.Round)
        drawRoundRect(AuraDockColors.Background, Offset(p(44f), p(30f)), Size(p(232f), p(108f)), CornerRadius(p(18f)))
        drawRoundRect(AuraDockColors.Inactive, Offset(p(44f), p(30f)), Size(p(232f), p(108f)), CornerRadius(p(18f)), style = line)
        val text = measurer.measure("12:30", style.copy(fontSize = (p(64f) / density).sp))
        drawText(text, topLeft = Offset(p(160f) - text.size.width / 2f, p(105f) - text.firstBaseline))
        val legs = Path().apply { moveTo(p(118f), p(138f)); lineTo(p(100f), p(186f)); moveTo(p(202f), p(138f)); lineTo(p(220f), p(186f)) }
        drawPath(legs, AuraDockColors.Inactive, style = line)
        drawRoundRect(AuraDockColors.SurfaceRaised, Offset(p(72f), p(186f)), Size(p(176f), p(10f)), CornerRadius(p(5f)))
        val cable = Path().apply { moveTo(p(276f), p(84f)); cubicTo(p(302f), p(84f), p(306f), p(150f), p(294f), p(206f)) }
        drawPath(cable, AuraDockColors.Outline, style = line)
    }
}
