package com.auradock.focus

/**
 * One moment of the phone's Do Not Disturb (DND) settings: which mode it is in (the "interruption filter") and the
 * priority rules (the "policy": who may still get through). These are plain numbers so the decisions below can be unit-tested
 * without a phone. The values are Android's own constants.
 */
data class DndSnapshot(
    val filter: Int,
    val categories: Int,
    val callSenders: Int,
    val messageSenders: Int,
    val conversationSenders: Int,
    val suppressedEffects: Int
)

/** NotificationManager.INTERRUPTION_FILTER_ALL: DND is off, everything gets through. 0 means "unknown". */
const val FILTER_ALL = 1
const val FILTER_PRIORITY = 2

/**
 * What we remember between docking and undocking, saved on disk so a crash cannot lose it.
 *  - [changed]: did *we* turn DND on? (false when it was already on: then we never touch it);
 *  - [saved]: the user's own settings from before we changed anything: exactly what we put back;
 *  - [applied]: what DND looked like right after we changed it: lets us notice if the user took over meanwhile.
 */
data class DockDndState(
    val changed: Boolean = false,
    val saved: DndSnapshot? = null,
    val applied: DndSnapshot? = null
)

sealed interface DndAction {
    /** Leave DND exactly as it is. */
    data object None : DndAction

    /** Remember [save] (the user's current settings), then switch to the focus settings. */
    data class Apply(val save: DndSnapshot) : DndAction

    /** Put the user's own settings back. */
    data class Restore(val snapshot: DndSnapshot) : DndAction

    /** Forget our record without touching DND (the user changed it themselves while docked). */
    data object Forget : DndAction
}

/**
 * The whole save-and-restore rule in one function.
 *
 * [wantFocus] is true when Focus is switched on, DND access is granted, and a dock is showing right now.
 *
 *  - Wanted and we have not changed anything yet:
 *      - DND is off: save the user's settings, then switch to focus mode ([DndAction.Apply]);
 *      - DND is already on (the user's own choice): do nothing, and we remember we did not touch it.
 *  - Wanted and we already changed it: nothing more to do.
 *  - Not wanted (the dock closed, Focus was turned off, or the app restarted after a crash) and we changed it:
 *      - if DND still looks the way we left it: put back the saved settings;
 *      - if it looks different, the user changed it meanwhile: do NOT overwrite their choice, just forget our record.
 *  - Not wanted and we never changed anything: nothing to do.
 */
fun planDnd(wantFocus: Boolean, state: DockDndState, current: DndSnapshot): DndAction = when {
    wantFocus && !state.changed ->
        if (current.filter == FILTER_ALL) DndAction.Apply(save = current) else DndAction.None

    wantFocus -> DndAction.None

    state.changed -> {
        val saved = state.saved
        // applied == null means we crashed between "save" and "apply was confirmed": assume it is still ours.
        val stillOurs = state.applied == null || current == state.applied
        if (saved != null && stillOurs) DndAction.Restore(saved) else DndAction.Forget
    }

    else -> DndAction.None
}
