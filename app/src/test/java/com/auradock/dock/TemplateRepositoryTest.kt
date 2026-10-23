package com.auradock.dock

import android.content.Context
import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class TemplateRepositoryTest {

    private lateinit var context: Context
    private lateinit var prefs: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    @Before
    fun setUp() {
        context = mock()
        prefs = mock()
        editor = mock()
        whenever(context.getSharedPreferences(any(), any())).thenReturn(prefs)
        whenever(prefs.edit()).thenReturn(editor)
        whenever(editor.putString(any(), any())).thenReturn(editor)
    }

    private fun repoWithStored(value: String?): TemplateRepository {
        whenever(prefs.getString(eq("dock_template"), anyOrNull())).thenReturn(value)
        return TemplateRepository(context)
    }

    // ── parsing the stored value ──────────────────────────────────────────────

    @Test
    fun `nothing stored gives the default`() = assertEquals(DockTemplate.CLASSIC, parseDockTemplate(null))

    @Test
    fun `garbage gives the default`() = assertEquals(DockTemplate.CLASSIC, parseDockTemplate("NOT_A_TEMPLATE"))

    @Test
    fun `an offered template is restored`() = assertEquals(DockTemplate.MINIMAL, parseDockTemplate("MINIMAL"))

    @Test
    fun `a template that is not built yet falls back to the default`() {
        assertEquals(DockTemplate.CLASSIC, parseDockTemplate("FOCUS"))
        assertEquals(DockTemplate.CLASSIC, parseDockTemplate("PRODUCTIVITY"))
    }

    @Test
    fun `the gallery offers only Classic and Minimal for now`() =
        assertEquals(listOf(DockTemplate.CLASSIC, DockTemplate.MINIMAL), DockTemplate.available)

    // ── the repository ────────────────────────────────────────────────────────

    @Test
    fun `restores the saved choice on creation`() =
        assertEquals(DockTemplate.MINIMAL, repoWithStored("MINIMAL").selected.value)

    @Test
    fun `starts with the default when nothing is saved`() =
        assertEquals(DockTemplate.CLASSIC, repoWithStored(null).selected.value)

    @Test
    fun `selecting updates the flow and writes it to the phone`() {
        val repo = repoWithStored(null)
        repo.select(DockTemplate.MINIMAL)
        assertEquals(DockTemplate.MINIMAL, repo.selected.value)
        verify(editor).putString("dock_template", "MINIMAL")
        verify(editor).apply()
    }
}
