package com.auradock.focus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PickedAppsTest {
    @Test
    fun `picking adds to the end`() = assertEquals(listOf("a", "b"), togglePick(listOf("a"), "b"))

    @Test
    fun `tapping a picked app un-picks it`() = assertEquals(listOf("b"), togglePick(listOf("a", "b"), "a"))

    @Test
    fun `a fourth app cannot be picked`() {
        val three = listOf("a", "b", "c")
        assertEquals(three, togglePick(three, "d"))
        assertFalse(canToggle(three, "d"))
    }

    @Test
    fun `with three picked you can still un-pick one, then pick another`() {
        val three = listOf("a", "b", "c")
        assertTrue(canToggle(three, "b"))
        val two = togglePick(three, "b")
        assertEquals(listOf("a", "c"), two)
        assertEquals(listOf("a", "c", "d"), togglePick(two, "d"))
    }

    @Test
    fun `the limit is three by default but can be changed`() {
        assertEquals(3, MAX_PICKED_APPS)
        assertEquals(listOf("a", "b"), togglePick(listOf("a", "b"), "c", max = 2))
    }

    @Test
    fun `apps are listed once each, sorted by name ignoring capitals`() {
        val sorted = sortedUniqueApps(
            listOf(AppEntry("p.zeta", "zeta"), AppEntry("p.alpha", "Alpha"), AppEntry("p.alpha", "Alpha"), AppEntry("p.beta", "beta"))
        )
        assertEquals(listOf("Alpha", "beta", "zeta"), sorted.map { it.label })
    }
}
