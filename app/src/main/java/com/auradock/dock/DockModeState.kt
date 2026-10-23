package com.auradock.dock

import com.auradock.standby.StandbyState

/**
 * When the in-app dock is on screen. Two things can show it:
 *  - **auto**: the phone is charging and in landscape ([StandbyState.ACTIVE], from StandbyTriggerManager);
 *  - **manual**: the user pressed "Dock now" ([manual]).
 *
 * A touch (or Back) closes the dock. Closing a manual dock just clears [manual]. Closing an auto dock sets
 * [autoDismissed], because the phone is still charging in landscape and would otherwise show the dock again
 * straight away; the dismissal is forgotten once those conditions end, so docking again later works normally.
 */
data class DockModeState(
    val manual: Boolean = false,
    val autoDismissed: Boolean = false
) {
    fun visible(auto: StandbyState): Boolean = manual || (auto == StandbyState.ACTIVE && !autoDismissed)

    fun enterManual(): DockModeState = copy(manual = true)

    fun dismiss(auto: StandbyState): DockModeState = when {
        manual -> copy(manual = false, autoDismissed = auto == StandbyState.ACTIVE)
        auto == StandbyState.ACTIVE -> copy(autoDismissed = true)
        else -> this
    }

    /** Call whenever the auto state changes. */
    fun onAutoChanged(auto: StandbyState): DockModeState =
        if (auto == StandbyState.INACTIVE) copy(autoDismissed = false) else this
}
