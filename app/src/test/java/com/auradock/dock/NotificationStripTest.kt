package com.auradock.dock

import com.auradock.data.NotificationItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationStripTest {
    private fun item(app: String = "WhatsApp", title: String = "Priya", content: String = "Dinner at 8?") =
        NotificationItem("k", "pkg", app, title, content, 1)

    @Test
    fun `by default only the app and sender show, never the message`() =
        assertEquals("WhatsApp · Priya", stripLine(item(), showText = false))

    @Test
    fun `with the toggle on the message text follows the sender`() =
        assertEquals("WhatsApp · Priya: Dinner at 8?", stripLine(item(), showText = true))

    @Test
    fun `an empty message adds nothing even with the toggle on`() =
        assertEquals("WhatsApp · Priya", stripLine(item(content = "  "), showText = true))

    @Test
    fun `no notifications means no lines, so no strip`() = assertTrue(stripLines(emptyList(), showText = true).isEmpty())

    @Test
    fun `one line per notification, in order`() =
        assertEquals(
            listOf("A · x", "B · y"),
            stripLines(listOf(item("A", "x"), item("B", "y")), showText = false)
        )
}
