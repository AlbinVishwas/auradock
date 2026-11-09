package com.auradock.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BoldPaletteTest {
    @Test fun `coral is the default and unknown names fall back to it`() {
        assertEquals(BoldPalette.Coral, BoldPalette.default)
        assertEquals(BoldPalette.Coral, BoldPalette.parse(null))
        assertEquals(BoldPalette.Coral, BoldPalette.parse("Nope"))
        assertEquals(BoldPalette.Violet, BoldPalette.parse("Violet"))
    }

    @Test fun `each colourway has two or three tones that alternate across the digits`() {
        BoldPalette.entries.forEach { assertTrue(it.tones.size in 2..3) }
        val blue = BoldPalette.Blue
        assertEquals(blue.tones[0], blue.tone(0))
        assertEquals(blue.tones[2], blue.tone(2))
        assertEquals(blue.tones[0], blue.tone(3))
        assertEquals(7, BoldPalette.entries.size)
    }
}
