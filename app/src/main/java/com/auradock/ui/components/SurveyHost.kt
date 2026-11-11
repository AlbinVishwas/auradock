package com.auradock.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.auradock.analytics.Analytics
import com.auradock.analytics.SurveyBridge
import com.auradock.ui.theme.AuraDockColors
import com.auradock.ui.theme.AuraDockSpacing
import com.auradock.ui.theme.AuraDockTypography

/**
 * Shows a PostHog survey ("What's missing?") as a sheet, on the dashboard only: it is placed in the dashboard's branch and nowhere else, so the dock
 * (and the screensaver) can never show one. A survey waits until the app has been used for 7 days and ignores anything but a single free-text question.
 */
@Composable
fun SurveyHost() {
    val context = LocalContext.current
    val pending by SurveyBridge.pending.collectAsState()
    val survey = pending ?: return
    if (!SurveyBridge.eligible(Analytics.firstUseAt(context), System.currentTimeMillis())) return
    var text by remember(survey) { mutableStateOf("") }
    LaunchedEffect(survey) { survey.onShown() }
    Box(Modifier.fillMaxSize()) {
        AuraSheet(onDismiss = { survey.dismiss(); SurveyBridge.clear() }) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = AuraDockSpacing.mdl, vertical = AuraDockSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(AuraDockSpacing.md)
            ) {
                Text(text = survey.question, style = AuraDockTypography.SheetTitle, color = AuraDockColors.OnBackground)
                AuraTextField(value = text, onValueChange = { text = it }, hint = "Anything at all", onDone = {})
                PrimaryPill(
                    text = survey.buttonText,
                    enabled = text.isNotBlank(),
                    onClick = { survey.send(text.trim()); SurveyBridge.clear() }
                )
                GreyTextButton(text = "Not now", onClick = { survey.dismiss(); SurveyBridge.clear() })
            }
        }
    }
}
