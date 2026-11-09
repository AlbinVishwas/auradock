package com.auradock.dock

import org.junit.Assert.assertEquals
import org.junit.Test

class UndoFocusRulesTest {
    @Test fun `the first touch with Focus on shows Undo focus`() {
        assertEquals(DockTap.SHOW_UNDO, tapOnDock(focusOn = true, undoShowing = false))
    }

    @Test fun `a second touch while it shows closes the dock`() {
        assertEquals(DockTap.CLOSE, tapOnDock(focusOn = true, undoShowing = true))
    }

    @Test fun `with no Focus to undo a touch closes the dock`() {
        assertEquals(DockTap.CLOSE, tapOnDock(focusOn = false, undoShowing = false))
    }
}
