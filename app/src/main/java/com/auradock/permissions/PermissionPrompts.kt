package com.auradock.permissions

import com.auradock.ui.onboarding.SetupPermission

/**
 * When AuraDock shows a permission card as a sheet. Nothing is asked up front: each permission is asked the first time the
 * feature that needs it is used ([askOnFirstUse]), once, and again only when the user taps an "Allow" for it themselves.
 */
object PermissionPrompts {
    /** Ask automatically? Only if it is not allowed and has not been asked about before. */
    fun askOnFirstUse(permission: SetupPermission, granted: Boolean, alreadyAsked: Set<String>): Boolean =
        !granted && permission.name !in alreadyAsked

    /** The first task with a time needs notifications, then exact alarms: whichever of the two is still to be asked, in that order. */
    fun forTimedTask(granted: Set<SetupPermission>, alreadyAsked: Set<String>): List<SetupPermission> =
        listOf(SetupPermission.POST_NOTIFICATIONS, SetupPermission.EXACT_ALARMS)
            .filter { askOnFirstUse(it, it in granted, alreadyAsked) }

    /** The first dock with Focus on needs Do Not Disturb access. */
    fun forFirstDock(focusOn: Boolean, granted: Set<SetupPermission>, alreadyAsked: Set<String>): List<SetupPermission> =
        if (focusOn && askOnFirstUse(SetupPermission.DND_ACCESS, SetupPermission.DND_ACCESS in granted, alreadyAsked))
            listOf(SetupPermission.DND_ACCESS) else emptyList()
}
