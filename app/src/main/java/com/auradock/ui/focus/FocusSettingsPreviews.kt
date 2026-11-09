package com.auradock.ui.focus

import androidx.compose.runtime.Composable
import com.auradock.focus.vip.Vip
import com.auradock.ui.preview.PreviewComponent
import com.auradock.ui.preview.PreviewData
import com.auradock.ui.preview.PreviewPortrait
import com.auradock.ui.theme.AuraDockTheme

private val Full = FocusSettingsUiState(
    focusWhileDocked = true,
    hasDndAccess = true,
    hasNotificationAccess = true,
    apps = PreviewData.apps,
    pickedApps = listOf("pkg.chat", "pkg.mail"),
    showMessageText = false,
    dockFromAnywhere = true,
    overlayGranted = true
)

@PreviewPortrait
@Composable
private fun FocusFull() = AuraDockTheme { FocusSettingsScreen(Full, FocusSettingsActions()) }

/** Everything off and no access granted: each card explains and offers to allow. */
@PreviewPortrait
@Composable
private fun FocusPermissionsMissing() = AuraDockTheme {
    FocusSettingsScreen(
        Full.copy(
            focusWhileDocked = true,
            hasDndAccess = false,
            hasNotificationAccess = false,
            pickedApps = emptyList(),
            dockFromAnywhere = true,
            overlayGranted = false
        ),
        FocusSettingsActions()
    )
}

/** The app list is still loading. */
@PreviewPortrait
@Composable
private fun FocusLoadingApps() = AuraDockTheme {
    FocusSettingsScreen(Full.copy(apps = null, pickedApps = emptyList()), FocusSettingsActions())
}

/** Three apps picked: the rest are greyed out. */
@PreviewPortrait
@Composable
private fun FocusMaxPicked() = AuraDockTheme {
    FocusSettingsScreen(
        Full.copy(pickedApps = listOf("pkg.calendar", "pkg.chat", "pkg.mail"), dockFromAnywhere = false),
        FocusSettingsActions()
    )
}

@PreviewComponent
@Composable
private fun DockFromAnywhereOff() = AuraDockTheme { DockFromAnywhereCard(enabled = false, overlayGranted = false, onToggle = {}, onAllowOverlay = {}) }

@PreviewComponent
@Composable
private fun DockFromAnywhereWaiting() = AuraDockTheme { DockFromAnywhereCard(enabled = true, overlayGranted = false, onToggle = {}, onAllowOverlay = {}) }

@PreviewComponent
@Composable
private fun DockFromAnywhereOn() = AuraDockTheme { DockFromAnywhereCard(enabled = true, overlayGranted = true, onToggle = {}, onAllowOverlay = {}) }

private val SomeVips = listOf(Vip("a", "Asha Rao"), Vip("b", "Mum"), Vip("c", "Dr. Fernandes"))

/** The card with nobody on it yet. */
@PreviewComponent
@Composable
private fun VipCardEmpty() = AuraDockTheme { VipCard(VipUiState(), VipActions()) }

/** Three people and ring-through on. */
@PreviewComponent
@Composable
private fun VipCardPeople() = AuraDockTheme { VipCard(VipUiState(vips = SomeVips, ringThrough = true), VipActions()) }

/** Five people (the limit): adding is closed. */
@PreviewComponent
@Composable
private fun VipCardFull() = AuraDockTheme {
    VipCard(VipUiState(vips = SomeVips + listOf(Vip("d", "Ravi"), Vip("e", "Priya")), ringThrough = true), VipActions())
}

/** Something was left on loan (the app was killed while docked): the card offers to put it back. */
@PreviewComponent
@Composable
private fun VipCardOnLoan() = AuraDockTheme { VipCard(VipUiState(vips = SomeVips, ringThrough = true, onLoan = 2), VipActions()) }

/** The whole Focus page with the card filled in (full-page preview). */
@PreviewPortrait
@Composable
private fun FocusWithPeople() = AuraDockTheme {
    FocusSettingsScreen(Full.copy(vip = VipUiState(vips = SomeVips, ringThrough = true)), FocusSettingsActions())
}

/** The one-time sheet the first time ring-through is switched on. */
@PreviewPortrait
@Composable
private fun FocusVipConfirm() = AuraDockTheme {
    FocusSettingsScreen(Full.copy(vip = VipUiState(vips = SomeVips, confirming = true)), FocusSettingsActions())
}
