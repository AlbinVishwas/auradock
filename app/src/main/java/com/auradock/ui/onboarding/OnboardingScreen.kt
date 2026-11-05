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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auradock.dock.DockTemplate
import com.auradock.dock.DockUiState
import com.auradock.ui.components.QuickActionButton
import com.auradock.ui.screens.TemplateGalleryScreen
import com.auradock.ui.theme.AuraDockColors
import com.auradock.ui.theme.AuraDockTypography

private enum class Step { WELCOME, SCREENSAVER, PERMISSIONS, TEMPLATE }

/**
 * The first-launch walkthrough: Welcome, set as screensaver, permissions, pick a template. [onFinish] runs after the
 * template is picked; the caller saves that the walkthrough is done and shows the dashboard.
 */
@Composable
fun OnboardingScreen(
    template: DockTemplate,
    dockState: DockUiState,
    onOpenScreensaverSettings: () -> Unit,
    onPickTemplate: (DockTemplate) -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    var stepIndex by rememberSaveable { mutableIntStateOf(0) }
    val step = Step.entries[stepIndex]
    BackHandler(enabled = stepIndex > 0) { stepIndex-- }

    when (step) {
        Step.TEMPLATE -> TemplateGalleryScreen(
            selected = template,
            state = dockState,
            modifier = modifier,
            onApplyClick = {
                onPickTemplate(it)
                onFinish()
            }
        )

        else -> Column(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 48.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = when (step) {
                    Step.WELCOME -> "Welcome to AuraDock"
                    Step.SCREENSAVER -> "Set AuraDock as your screensaver"
                    else -> "Permissions"
                },
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = when (step) {
                    Step.WELCOME -> "Turn your phone into a dock for the time, your tasks and your day while it charges on a stand."
                    Step.SCREENSAVER -> "AuraDock appears in your phone's screensaver list and starts by itself while the phone charges. " +
                        "Open the settings and choose AuraDock."
                    else -> "AuraDock asks for a few things so its features work. You can skip any of them and allow them later."
                },
                style = AuraDockTypography.WidgetPrimary,
                color = AuraDockColors.OnSurfaceVariant
            )
            if (step == Step.SCREENSAVER) {
                Text(
                    text = "Tip: to see it, put the phone on its stand while charging and let the screen time out.",
                    style = AuraDockTypography.Caption,
                    color = AuraDockColors.OnSurfaceVariant
                )
                QuickActionButton(text = "Open screensaver settings", onClick = onOpenScreensaverSettings, isActive = true)
            }
            Spacer(Modifier.height(8.dp))
            QuickActionButton(text = if (step == Step.WELCOME) "Get started" else "Next", onClick = { stepIndex++ })
        }
    }
}
