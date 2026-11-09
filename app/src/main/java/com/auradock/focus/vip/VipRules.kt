package com.auradock.focus.vip

/** At most this many people can always reach the user. */
const val MAX_VIPS = 5

/**
 * One person who can always reach the user. Only the contact's lookup key (Android's stable id for a contact, which survives
 * edits and merges) and the name as it was when picked are kept, on the phone only.
 */
data class Vip(val lookupKey: String, val name: String)

/** [vip] added to the end, unless the list is full or already holds that contact. */
fun addVip(list: List<Vip>, vip: Vip): List<Vip> =
    if (list.size >= MAX_VIPS || list.any { it.lookupKey == vip.lookupKey }) list else list + vip

fun removeVip(list: List<Vip>, lookupKey: String): List<Vip> = list.filterNot { it.lookupKey == lookupKey }

fun canAddVip(list: List<Vip>): Boolean = list.size < MAX_VIPS

/** The list as one line for the phone's preferences: `key<TAB>name` per person, one person per line. Tabs and newlines in a name are spaces. */
fun encodeVips(list: List<Vip>): String =
    list.joinToString("\n") { "${it.lookupKey.clean()}\t${it.name.clean()}" }

fun decodeVips(text: String): List<Vip> =
    text.lines().mapNotNull { line ->
        val key = line.substringBefore('\t', "").trim()
        val name = line.substringAfter('\t', "").trim()
        if (key.isEmpty()) null else Vip(key, name)
    }.distinctBy { it.lookupKey }.take(MAX_VIPS)

private fun String.clean() = replace('\t', ' ').replace('\n', ' ').trim()

/**
 * What the loan did and must undo: the lookup keys of the VIPs that AuraDock marked as favourites in Contacts because they
 * were not already. People who were favourites before are never in this list, so they are never changed back.
 */
data class LoanPlan(val toMark: List<String>)

/**
 * Which VIPs need marking while docked. [alreadyMarked] holds the lookup keys of VIPs that are favourites right now (their
 * own choice, or one we already loaned). [journal] is what we loaned earlier and have not given back yet.
 */
fun planLend(vips: List<Vip>, alreadyMarked: Set<String>): LoanPlan =
    LoanPlan(vips.map { it.lookupKey }.filter { it !in alreadyMarked })

/**
 * Which people to give back: everyone in the [journal] who is still a favourite. A person who is no longer a favourite
 * (the user changed it themselves meanwhile) is left alone, but still leaves the journal.
 */
fun planReturn(journal: Set<String>, markedNow: Set<String>): List<String> = journal.filter { it in markedNow }

/**
 * Whether a notification is from a VIP. [personUris] are the contact links the sending app attached (a messaging app names
 * its sender that way; a link holds the contact's lookup key), [title] is the notification's title, which most messaging
 * apps set to the sender's name. A name only counts if it matches exactly, ignoring case.
 */
fun isFromVip(vips: List<Vip>, personUris: List<String>, title: String): Boolean {
    if (vips.isEmpty()) return false
    val name = title.trim()
    return vips.any { vip ->
        personUris.any { uri -> "/lookup/${vip.lookupKey}" in uri } ||
            (name.isNotEmpty() && vip.name.isNotBlank() && name.equals(vip.name.trim(), ignoreCase = true))
    }
}
