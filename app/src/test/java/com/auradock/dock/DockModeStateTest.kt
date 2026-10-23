package com.auradock.dock

import com.auradock.standby.StandbyState.ACTIVE
import com.auradock.standby.StandbyState.INACTIVE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DockModeStateTest {

    @Test
    fun `hidden by default`() = assertFalse(DockModeState().visible(INACTIVE))

    @Test
    fun `auto dock shows when charging in landscape`() = assertTrue(DockModeState().visible(ACTIVE))

    @Test
    fun `Dock now shows the dock even when not charging`() =
        assertTrue(DockModeState().enterManual().visible(INACTIVE))

    @Test
    fun `touch closes a manual dock`() {
        val closed = DockModeState().enterManual().dismiss(INACTIVE)
        assertFalse(closed.visible(INACTIVE))
        assertFalse(closed.manual)
    }

    @Test
    fun `touch closes an auto dock and it stays closed while still charging in landscape`() {
        val closed = DockModeState().dismiss(ACTIVE)
        assertFalse(closed.visible(ACTIVE))
    }

    @Test
    fun `an auto dock dismissal is forgotten once charging or landscape ends`() {
        val closed = DockModeState().dismiss(ACTIVE)
        val reset = closed.onAutoChanged(INACTIVE)
        assertFalse(reset.autoDismissed)
        assertTrue(reset.visible(ACTIVE)) // docking again later shows the dock
    }

    @Test
    fun `closing a manual dock while auto is also active closes both`() {
        val closed = DockModeState().enterManual().dismiss(ACTIVE)
        assertFalse(closed.visible(ACTIVE))
    }

    @Test
    fun `auto becoming active does not clear a dismissal`() {
        val closed = DockModeState().dismiss(ACTIVE)
        assertEquals(closed, closed.onAutoChanged(ACTIVE))
    }

    @Test
    fun `dismiss with nothing showing changes nothing`() {
        val s = DockModeState()
        assertEquals(s, s.dismiss(INACTIVE))
    }
}
