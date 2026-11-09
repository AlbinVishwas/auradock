package com.auradock.dock

/** What a single tap on the dock does. */
enum class DockTap {
    /** Show the small "Undo focus" control for a few seconds (Focus is on and not already showing the control). */
    SHOW_UNDO,

    /** Nothing: a single tap never leaves the dock (a double tap does). */
    NONE
}

/** A single tap only shows "Undo focus", and only when there is a Focus to undo and the control is not already showing. */
fun tapOnDock(focusOn: Boolean, undoShowing: Boolean): DockTap =
    if (focusOn && !undoShowing) DockTap.SHOW_UNDO else DockTap.NONE
