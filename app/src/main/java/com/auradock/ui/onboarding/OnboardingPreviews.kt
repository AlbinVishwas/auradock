package com.auradock.ui.onboarding

import androidx.compose.runtime.Composable
import com.auradock.dock.DockTemplate
import com.auradock.screensaver.ScreensaverTestUiState
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

/** The first dock, full screen, for about five seconds. */
@PreviewPortrait
@Composable
private fun OnboardingDockPreview() = AuraDockTheme { OnboardingScreen(NothingGranted, OnboardingActions(), initialStep = 1) }

@PreviewPortrait
@Composable
private fun OnboardingTemplate() = AuraDockTheme { OnboardingScreen(NothingGranted, OnboardingActions(), initialStep = 2) }

@PreviewPortrait
@Composable
private fun OnboardingScreensaverStep() = AuraDockTheme { OnboardingScreen(NothingGranted, OnboardingActions(), initialStep = 3) }

@PreviewPortrait
@Composable
private fun OnboardingDockFromAnywhereStep() = AuraDockTheme { OnboardingScreen(NothingGranted, OnboardingActions(), initialStep = 4) }

@PreviewPortrait
@Composable
private fun OnboardingDockFromAnywhereOn() = AuraDockTheme { OnboardingScreen(EverythingGranted, OnboardingActions(), initialStep = 4) }

/** A permission asked when its feature is first used: the card as a sheet. */
@PreviewPortrait
@Composable
private fun PermissionSheetCalendar() = AuraDockTheme { PermissionSheet(SetupPermission.CALENDAR, onAllow = {}, onSkip = {}) }

@PreviewPortrait
@Composable
private fun PermissionSheetContacts() = AuraDockTheme { PermissionSheet(SetupPermission.CONTACTS, onAllow = {}, onSkip = {}) }

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

private val TestSteps1 = ScreensaverTestUiState(selected = true, timeoutOk = false)
private val TestAppeared = ScreensaverTestUiState(selected = true, timeoutOk = true, appeared = true)

/** The walkthrough's screensaver step, one step done. */
@PreviewPortrait
@Composable
private fun OnboardingScreensaverTestOneDone() = AuraDockTheme {
    OnboardingScreen(NothingGranted.copy(screensaverTest = TestSteps1), OnboardingActions(), initialStep = 3)
}

/** Waiting on the stand: the third step still open and no button but Continue. */
@PreviewPortrait
@Composable
private fun OnboardingScreensaverTestWaiting() = AuraDockTheme {
    OnboardingScreen(NothingGranted.copy(screensaverTest = ScreensaverTestUiState(selected = true, timeoutOk = true)), OnboardingActions(), initialStep = 3)
}

@PreviewPortrait
@Composable
private fun ScreensaverTestPageStart() = AuraDockTheme { ScreensaverTestScreen(ScreensaverTestUiState(), ScreensaverTestActions()) }

@PreviewPortrait
@Composable
private fun ScreensaverTestPageAppeared() = AuraDockTheme { ScreensaverTestScreen(TestAppeared, ScreensaverTestActions()) }
