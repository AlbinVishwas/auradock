package com.auradock.dock

import org.junit.Assert.assertEquals
import org.junit.Test

class UndoFocusRulesTest {
    @Test fun `the first touch with Focus on shows Undo focus`() {
        assertEquals(DockTap.SHOW_UNDO, tapOnDock(focusOn = true, undoShowing = false))
    }

    @Test fun `a second touch while it shows does nothing the dock`() {
        assertEquals(DockTap.NONE, tapOnDock(focusOn = true, undoShowing = true))
    }

    @Test fun `with no Focus to undo a touch does nothing the dock`() {
        assertEquals(DockTap.NONE, tapOnDock(focusOn = false, undoShowing = false))
    }
}
