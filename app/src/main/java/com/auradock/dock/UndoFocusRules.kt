package com.auradock.dock

/** What a touch on the in-app dock does. */
enum class DockTap {
    /** Show the small "Undo focus" control for a few seconds (Focus is on and not already showing the control). */
    SHOW_UNDO,

    /** Close the dock, as before. */
    CLOSE
}

/**
 * A touch shows "Undo focus" first, when there is a Focus to undo and the control is not already showing. A second touch (while
 * it shows), or any touch when Focus is off or already undone, closes the dock.
 */
fun tapOnDock(focusOn: Boolean, undoShowing: Boolean): DockTap =
    if (focusOn && !undoShowing) DockTap.SHOW_UNDO else DockTap.CLOSE
