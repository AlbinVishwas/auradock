package com.auradock.ui.onboarding

import androidx.compose.runtime.Composable
import com.auradock.dock.DockTemplate
import com.auradock.ui.preview.PreviewComponent
import com.auradock.ui.preview.PreviewData
import com.auradock.ui.preview.PreviewPortrait
import com.auradock.ui.screens.TemplateGalleryUiState
import com.auradock.ui.theme.AuraDockTheme

private val NothingGranted = OnboardingUiState(
    granted = emptySet(),
    screensaverSelected = false,
    dockFromAnywhere = false,
    overlayGranted = false,
    gallery = TemplateGalleryUiState(DockTemplate.CLASSIC, PreviewData.dockFull)
)

private val EverythingGranted = NothingGranted.copy(
    granted = SetupPermission.entries.toSet(),
    screensaverSelected = true,
    dockFromAnywhere = true,
    overlayGranted = true
)

@PreviewPortrait
@Composable
private fun OnboardingWelcome() = AuraDockTheme { OnboardingScreen(NothingGranted, OnboardingActions(), initialStep = 0) }

@PreviewPortrait
@Composable
private fun OnboardingDockStepNotChosen() = AuraDockTheme { OnboardingScreen(NothingGranted, OnboardingActions(), initialStep = 1) }

@PreviewPortrait
@Composable
private fun OnboardingDockStepChosen() = AuraDockTheme { OnboardingScreen(EverythingGranted, OnboardingActions(), initialStep = 1) }

@PreviewPortrait
@Composable
private fun OnboardingDockFromAnywhere() = AuraDockTheme { OnboardingScreen(NothingGranted, OnboardingActions(), initialStep = 2) }

/** Permission step with nothing allowed yet. */
@PreviewPortrait
@Composable
private fun OnboardingPermissionsMissing() = AuraDockTheme { OnboardingScreen(NothingGranted, OnboardingActions(), initialStep = 3) }

/** Permission step with everything allowed: every card shows its checkmark. */
@PreviewPortrait
@Composable
private fun OnboardingPermissionsGranted() = AuraDockTheme { OnboardingScreen(EverythingGranted, OnboardingActions(), initialStep = 3) }

@PreviewPortrait
@Composable
private fun OnboardingTemplate() = AuraDockTheme { OnboardingScreen(NothingGranted, OnboardingActions(), initialStep = 4) }

@PreviewPortrait
@Composable
private fun SetupNothingGranted() = AuraDockTheme { SetupScreen(SetupUiState(emptySet(), screensaverSelected = false), SetupActions()) }

@PreviewPortrait
@Composable
private fun SetupEverythingGranted() = AuraDockTheme {
    SetupScreen(SetupUiState(SetupPermission.entries.toSet(), screensaverSelected = true), SetupActions())
}

@PreviewPortrait
@Composable
private fun SetupSomeGranted() = AuraDockTheme {
    SetupScreen(SetupUiState(setOf(SetupPermission.CALENDAR, SetupPermission.POST_NOTIFICATIONS), screensaverSelected = false), SetupActions())
}

@PreviewComponent
@Composable
private fun PermissionCardNotGranted() = AuraDockTheme { PermissionCard(SetupPermission.CALENDAR, granted = false, onAllow = {}) }

@PreviewComponent
@Composable
private fun PermissionCardGranted() = AuraDockTheme { PermissionCard(SetupPermission.DND_ACCESS, granted = true, onAllow = {}) }
