package com.auradock.focus

import org.junit.Assert.assertEquals
import org.junit.Test

class DndPlanTest {
    // DND off, with the user's usual priority rules
    private val off = DndSnapshot(filter = FILTER_ALL, categories = 4, callSenders = 1, messageSenders = 1, conversationSenders = 1, suppressedEffects = 7)
    // DND the user turned on themselves (priority mode with their own rules)
    private val userOn = DndSnapshot(filter = FILTER_PRIORITY, categories = 16, callSenders = 0, messageSenders = 0, conversationSenders = 2, suppressedEffects = 3)
    // what we set when focusing
    private val focused = DndSnapshot(filter = FILTER_PRIORITY, categories = 40, callSenders = 2, messageSenders = 2, conversationSenders = 3, suppressedEffects = 0)

    private val clean = DockDndState()
    private val weChanged = DockDndState(changed = true, saved = off, applied = focused)

    // ── entering the dock ─────────────────────────────────────────────────────

    @Test
    fun `docking with DND off saves the user's settings and applies focus`() =
        assertEquals(DndAction.Apply(save = off), planDnd(wantFocus = true, state = clean, current = off))

    @Test
    fun `docking with DND already on changes nothing`() =
        assertEquals(DndAction.None, planDnd(true, clean, userOn))

    @Test
    fun `docking again while we already changed it does nothing more`() =
        assertEquals(DndAction.None, planDnd(true, weChanged, focused))

    @Test
    fun `an unknown DND mode is treated as the user's and not touched`() =
        assertEquals(DndAction.None, planDnd(true, clean, off.copy(filter = 0)))

    // ── leaving the dock ──────────────────────────────────────────────────────

    @Test
    fun `leaving restores exactly what was saved`() =
        assertEquals(DndAction.Restore(off), planDnd(wantFocus = false, state = weChanged, current = focused))

    @Test
    fun `leaving does nothing if we never changed anything`() {
        assertEquals(DndAction.None, planDnd(false, clean, off))
        assertEquals(DndAction.None, planDnd(false, clean, userOn)) // their own DND stays on
    }

    @Test
    fun `a pre-existing DND is never turned off by leaving`() {
        // docked while DND was already on: nothing was applied, nothing is restored
        val afterDock = planDnd(true, clean, userOn)
        assertEquals(DndAction.None, afterDock)
        assertEquals(DndAction.None, planDnd(false, clean, userOn))
    }

    @Test
    fun `if the user turned DND off while docked we do not turn anything back on`() =
        assertEquals(DndAction.Forget, planDnd(false, weChanged, off))

    @Test
    fun `if the user changed DND to something else while docked we leave their choice`() =
        assertEquals(DndAction.Forget, planDnd(false, weChanged, userOn))

    // ── crash safety ──────────────────────────────────────────────────────────

    @Test
    fun `after a crash with no dock showing, our change is undone`() =
        // fresh process: nothing is docked, the saved state says we changed it, DND still looks like ours
        assertEquals(DndAction.Restore(off), planDnd(false, weChanged, focused))

    @Test
    fun `a crash between saving and confirming the change is still restored`() {
        val unconfirmed = DockDndState(changed = true, saved = off, applied = null)
        assertEquals(DndAction.Restore(off), planDnd(false, unconfirmed, focused))
        assertEquals(DndAction.Restore(off), planDnd(false, unconfirmed, off)) // restoring over an unchanged DND is harmless
    }

    @Test
    fun `a damaged record without saved settings is dropped rather than guessed at`() =
        assertEquals(DndAction.Forget, planDnd(false, DockDndState(changed = true, saved = null, applied = focused), focused))

    // ── the whole day ─────────────────────────────────────────────────────────

    @Test
    fun `dock, undock, dock, undock behaves the same each time`() {
        var state = clean
        var dnd = off
        repeat(2) {
            val apply = planDnd(true, state, dnd) as DndAction.Apply
            state = DockDndState(changed = true, saved = apply.save, applied = focused)
            dnd = focused
            val restore = planDnd(false, state, dnd) as DndAction.Restore
            dnd = restore.snapshot
            state = clean
            assertEquals(off, dnd)
        }
    }
}
