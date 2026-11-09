package com.auradock.focus

import com.auradock.focus.vip.MAX_VIPS
import com.auradock.focus.vip.Vip
import com.auradock.focus.vip.addVip
import com.auradock.focus.vip.canAddVip
import com.auradock.focus.vip.decodeVips
import com.auradock.focus.vip.encodeVips
import com.auradock.focus.vip.isFromVip
import com.auradock.focus.vip.planLend
import com.auradock.focus.vip.planReturn
import com.auradock.focus.vip.removeVip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VipRulesTest {
    private fun vip(n: Int) = Vip("key$n", "Person $n")

    @Test fun `adds up to five and no more`() {
        var list = emptyList<Vip>()
        (1..MAX_VIPS).forEach { list = addVip(list, vip(it)) }
        assertEquals(5, list.size)
        assertFalse(canAddVip(list))
        assertEquals(list, addVip(list, vip(6)))
    }

    @Test fun `the same contact is not added twice`() {
        val list = addVip(listOf(vip(1)), Vip("key1", "Renamed"))
        assertEquals(listOf(vip(1)), list)
    }

    @Test fun `removing frees a place`() {
        val list = (1..5).map(::vip)
        val smaller = removeVip(list, "key3")
        assertEquals(4, smaller.size)
        assertTrue(canAddVip(smaller))
    }

    @Test fun `the list survives being saved and read back`() {
        val list = listOf(Vip("0r1-2A", "Asha K"), Vip("3x9", "Tab\tand\nline"))
        assertEquals(listOf(Vip("0r1-2A", "Asha K"), Vip("3x9", "Tab and line")), decodeVips(encodeVips(list)))
    }

    @Test fun `reading an empty or damaged line gives no one`() {
        assertEquals(emptyList<Vip>(), decodeVips(""))
        assertEquals(emptyList<Vip>(), decodeVips("k\n\t\nnot a person")) // no tab, or no key: damaged, ignored
        assertEquals(listOf(Vip("k", "")), decodeVips("k\t"))
    }

    @Test fun `only people who are not favourites already get marked`() {
        val vips = listOf(vip(1), vip(2), vip(3))
        assertEquals(listOf("key2"), planLend(vips, alreadyMarked = setOf("key1", "key3")).toMark)
        assertEquals(emptyList<String>(), planLend(vips, alreadyMarked = setOf("key1", "key2", "key3")).toMark)
    }

    @Test fun `giving back touches only what was loaned and is still marked`() {
        assertEquals(listOf("key2"), planReturn(journal = setOf("key2", "key4"), markedNow = setOf("key1", "key2")))
        assertEquals(emptyList<String>(), planReturn(journal = emptySet(), markedNow = setOf("key1")))
    }

    @Test fun `a message is from a VIP by its contact link`() {
        val vips = listOf(Vip("0r1-2A", "Asha"))
        assertTrue(isFromVip(vips, listOf("content://com.android.contacts/contacts/lookup/0r1-2A/17"), "Someone else"))
        assertFalse(isFromVip(vips, listOf("content://com.android.contacts/contacts/lookup/9z/4"), "Someone else"))
    }

    @Test fun `or by the exact name in the title, ignoring case`() {
        val vips = listOf(Vip("k", "Asha Rao"))
        assertTrue(isFromVip(vips, emptyList(), "asha rao"))
        assertFalse(isFromVip(vips, emptyList(), "Asha"))
        assertFalse(isFromVip(vips, emptyList(), ""))
    }

    @Test fun `no VIPs means nothing matches`() {
        assertFalse(isFromVip(emptyList(), listOf("content://x/lookup/k/1"), "Asha"))
    }
}
