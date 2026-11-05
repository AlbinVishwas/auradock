package com.auradock.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationRulesTest {
    private fun raw(
        key: String = "k", pkg: String = "com.chat", title: String = "Priya", text: String = "hi",
        at: Long = 1, ongoing: Boolean = false, summary: Boolean = false
    ) = RawNotification(key, pkg, "Chat", title, text, at, ongoing, summary)

    private fun item(key: String, pkg: String = "com.chat", at: Long = 1) = NotificationItem(key, pkg, "Chat", "t", "c", at)

    // ── which notifications count ─────────────────────────────────────────────

    @Test
    fun `only picked apps count`() {
        assertTrue(isRelevant(raw(pkg = "com.chat"), setOf("com.chat")))
        assertFalse(isRelevant(raw(pkg = "com.other"), setOf("com.chat")))
        assertFalse(isRelevant(raw(), emptySet())) // nothing picked, nothing shown
    }

    @Test
    fun `ongoing notifications and group headers are ignored`() {
        val picked = setOf("com.chat")
        assertFalse(isRelevant(raw(ongoing = true), picked))
        assertFalse(isRelevant(raw(summary = true), picked))
    }

    @Test
    fun `an empty notification is ignored`() = assertFalse(isRelevant(raw(title = " ", text = ""), setOf("com.chat")))

    @Test
    fun `a missing title falls back to the app name`() {
        assertEquals("Chat", raw(title = "", text = "message").toItem().title)
        assertEquals("Priya", raw(title = "Priya").toItem().title)
    }

    // ── the cache ─────────────────────────────────────────────────────────────

    @Test
    fun `newest first`() =
        assertEquals(listOf("b", "a"), upsert(listOf(item("a", at = 1)), item("b", at = 2)).map { it.key })

    @Test
    fun `an update replaces the old version instead of adding a second`() {
        val updated = upsert(listOf(item("a", at = 1)), item("a", at = 5).copy(content = "new"))
        assertEquals(1, updated.size)
        assertEquals("new", updated.single().content)
    }

    @Test
    fun `the cache is capped, dropping the oldest`() {
        var items = emptyList<NotificationItem>()
        (1..25L).forEach { items = upsert(items, item("k$it", at = it), max = 20) }
        assertEquals(20, items.size)
        assertEquals("k25", items.first().key)
        assertEquals("k6", items.last().key)
    }

    @Test
    fun `a dismissed notification leaves the cache`() {
        val items = listOf(item("a"), item("b"))
        assertEquals(listOf("b"), removeByKey(items, "a").map { it.key })
        assertEquals(items, removeByKey(items, "missing"))
    }

    @Test
    fun `dropping an app removes its notifications`() {
        val items = listOf(item("a", "com.one"), item("b", "com.two"))
        assertEquals(listOf("b"), retainPicked(items, setOf("com.two")).map { it.key })
    }

    // ── what the strip shows ──────────────────────────────────────────────────

    @Test
    fun `latest per app, newest first`() {
        val items = listOf(item("a1", "com.one", 1), item("a2", "com.one", 5), item("b1", "com.two", 3))
        assertEquals(listOf("a2", "b1"), latestPerApp(items, listOf("com.one", "com.two")).map { it.key })
    }

    @Test
    fun `an app with nothing to show is simply missing`() {
        val items = listOf(item("a", "com.one", 1))
        assertEquals(listOf("a"), latestPerApp(items, listOf("com.one", "com.silent")).map { it.key })
        assertEquals(emptyList<NotificationItem>(), latestPerApp(emptyList(), listOf("com.one")))
    }
}
