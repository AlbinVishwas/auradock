package com.auradock.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.auradock.screensaver.ScreensaverNext
import com.auradock.screensaver.ScreensaverTestUiState
import com.auradock.screensaver.nextScreensaverStep
import com.auradock.ui.components.BackArrow
import com.auradock.ui.components.PrimaryPill
import com.auradock.ui.theme.AuraDockColors
import com.auradock.ui.theme.AuraDockRadii
import com.auradock.ui.theme.AuraDockSizes
import com.auradock.ui.theme.AuraDockSpacing
import com.auradock.ui.theme.AuraDockTypography
import com.auradock.ui.theme.fromScreenBottom
import com.auradock.ui.theme.fromScreenTop

/** What the user can do in the guided screensaver test. */
data class ScreensaverTestActions(
    /** Opens the system's screensaver settings (step 1). */
    val onChoose: () -> Unit = {},
    /** Opens the system's display settings, where the screen timeout is (step 2). */
    val onSetTimeout: () -> Unit = {}
)

/** The label and action of the one pill under the steps: the first step still to do. Null once the test is done. */
fun screensaverTestPill(state: ScreensaverTestUiState, actions: ScreensaverTestActions): Pair<String, () -> Unit>? =
    when (nextScreensaverStep(state.selected, state.timeoutOk, state.appeared)) {
        ScreensaverNext.CHOOSE -> "Choose AuraDock as screensaver" to actions.onChoose
        ScreensaverNext.TIMEOUT -> "Set screen timeout" to actions.onSetTimeout
        ScreensaverNext.WAIT, ScreensaverNext.DONE -> null
    }

/**
 * The guided test, three steps: choose AuraDock as screensaver, set the screen timeout, then put the phone on the stand and wait.
 * The third step is done when the screensaver has really started by itself (reported by the dream). Used in the walkthrough and
 * on its own page from Setup.
 */
@Composable
fun ScreensaverTestSteps(state: ScreensaverTestUiState, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AuraDockSpacing.sm)) {
        TestStep(1, "Choose AuraDock as screensaver", null, state.selected)
        TestStep(2, "Set screen timeout", "A minute or two is enough.", state.timeoutOk)
        if (state.appeared) {
            TestStep(3, "AuraDock appeared on its own", null, true)
        } else {
            TestStep(3, "Now put your phone on the stand and wait.", "Plugged in and sideways. It starts by itself, and the app tells you when it did.", false)
        }
    }
}

@Composable
private fun TestStep(number: Int, title: String, detail: String?, done: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AuraDockRadii.row))
            .background(AuraDockColors.Surface)
            .padding(horizontal = AuraDockSpacing.md, vertical = AuraDockSpacing.smd + AuraDockSpacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AuraDockSpacing.smd + AuraDockSpacing.xxs)
    ) {
        Box(
            Modifier.size(AuraDockSizes.allowedCheck).clip(CircleShape).background(if (done) AuraDockColors.Accent else AuraDockColors.Field),
            contentAlignment = Alignment.Center
        ) {
            if (done) Icon(Icons.Rounded.Check, contentDescription = "Done", tint = AuraDockColors.OnAccent, modifier = Modifier.size(AuraDockSizes.allowedCheckIcon))
            else Text(text = number.toString(), style = AuraDockTypography.RowCaption, color = AuraDockColors.OnSurfaceVariant)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = AuraDockTypography.Body, color = if (done) AuraDockColors.OnSurfaceVariant else AuraDockColors.OnBackground)
            if (detail != null) {
                Text(text = detail, style = AuraDockTypography.RowCaption, color = AuraDockColors.OnSurfaceVariant, modifier = Modifier.padding(top = AuraDockSpacing.xxs))
            }
        }
    }
}

/** The test on its own page (opened from Setup, and from the dashboard's "Try again"). */
@Composable
fun ScreensaverTestScreen(
    state: ScreensaverTestUiState,
    actions: ScreensaverTestActions,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AuraDockColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(start = AuraDockSpacing.mdl, end = AuraDockSpacing.mdl, top = fromScreenTop(AuraDockSpacing.xxl), bottom = fromScreenBottom(AuraDockSpacing.xxl - AuraDockSpacing.sm)),
        verticalArrangement = Arrangement.spacedBy(AuraDockSpacing.md)
    ) {
        BackArrow(Modifier.offset(x = -AuraDockSpacing.sm))
        Column(modifier = Modifier.padding(horizontal = AuraDockSpacing.sm)) {
            Text(text = "Screensaver test", color = AuraDockColors.OnBackground, style = AuraDockTypography.StepTitle)
            Text(
                text = "AuraDock opens on its own when the phone is charging, sideways on a stand, and the screen times out.",
                color = AuraDockColors.OnSurfaceVariant,
                style = AuraDockTypography.Secondary,
                modifier = Modifier.padding(top = AuraDockSpacing.smd)
            )
        }
        ScreensaverTestSteps(state)
        screensaverTestPill(state, actions)?.let { (label, onClick) -> PrimaryPill(text = label, onClick = onClick) }
    }
}
