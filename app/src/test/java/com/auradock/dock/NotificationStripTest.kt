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

class HeldCaptionTest {
    @org.junit.Test fun `counts are shown together`() = org.junit.Assert.assertEquals("6 held · 2 through", heldCaption(6, 2))
    @org.junit.Test fun `a quiet session shows nothing`() = org.junit.Assert.assertNull(heldCaption(0, 0))
    @org.junit.Test fun `one side can be zero`() {
        org.junit.Assert.assertEquals("0 held · 3 through", heldCaption(0, 3))
        org.junit.Assert.assertEquals("4 held · 0 through", heldCaption(4, 0))
    }
    @org.junit.Test fun `negative counts are never shown`() = org.junit.Assert.assertEquals("0 held · 1 through", heldCaption(-2, 1))
}
