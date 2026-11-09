package com.auradock.focus.vip

import com.auradock.monitoring.Monitoring
import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract.Contacts
import android.util.Log
import androidx.core.content.ContextCompat
import com.auradock.focus.FocusSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The thin Android layer for "People who can always reach you": it reads the picked contact and, while docked, lends the
 * VIPs a favourite mark in Contacts so Android's Do Not Disturb lets their calls and messages through (Android has no way to
 * name individual people for Do Not Disturb, only "favourites"). All decisions are in [planLend] and [planReturn].
 *
 * Safety:
 *  - Only the favourite flag is ever changed, never a name, number or anything else.
 *  - A person is written to the journal (on disk, committed) *before* we touch them, and only people who were not favourites
 *    already are ever touched, so nobody's own favourite is lost.
 *  - [giveBack] runs when the dock closes, at app start, at boot, and from the "Put back now" button; it is safe to repeat.
 */
@Singleton
class VipContacts @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: FocusSettings
) {
    private val resolver = context.contentResolver
    private val journalPrefs = context.getSharedPreferences("auradock_vip_loan", Context.MODE_PRIVATE)
    // One thread for every contacts write, so a "lend" and a "give back" can never run at the same time or out of order.
    private val worker = Executors.newSingleThreadExecutor()

    private val _onLoan = MutableStateFlow(journal().size)

    /** How many people are marked right now only because AuraDock lent the mark. Shown so the user can put them back. */
    val onLoan: StateFlow<Int> = _onLoan.asStateFlow()

    fun canRead(): Boolean = granted(Manifest.permission.READ_CONTACTS)
    fun canWrite(): Boolean = granted(Manifest.permission.WRITE_CONTACTS)

    private fun granted(permission: String) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    /** The contact the user picked (a `content://com.android.contacts/contacts/...` link), or null if it cannot be read. */
    fun read(picked: Uri): Vip? = runCatching {
        resolver.query(picked, arrayOf(Contacts.LOOKUP_KEY, Contacts.DISPLAY_NAME_PRIMARY), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0)?.let { Vip(it, c.getString(1).orEmpty()) } else null
        }
    }.getOrNull()

    /** Marks the VIPs who are not favourites yet. Needs the write permission and a switch that is on; otherwise does nothing. */
    fun lend() {
        if (!settings.vipRingThrough.value || !canRead() || !canWrite()) return
        val vips = settings.vips.value
        worker.execute { runCatching { lendNow(vips) }.onFailure { Log.w(TAG, "could not lend", it) } }
    }

    /** Puts every loaned mark back and empties the journal. Safe to call any time, as often as wanted. */
    fun giveBack() {
        if (journal().isEmpty()) return
        worker.execute { runCatching { giveBackNow() }.onFailure { Log.w(TAG, "could not give back", it) } }
    }

    private fun lendNow(vips: List<Vip>) {
        val marked = vips.mapNotNull { vip -> find(vip.lookupKey)?.takeIf { it.starred }?.let { vip.lookupKey } }.toSet()
        val plan = planLend(vips, alreadyMarked = marked)
        for (key in plan.toMark) {
            val contact = find(key) ?: continue // deleted since it was picked
            Monitoring.breadcrumb("vip", "lent", "count" to 1)
            setJournal(journal() + key) // written down first...
            if (!setStarred(contact.id, true)) setJournal(journal() - key) // ...and forgotten again if it did not take
        }
    }

    private fun giveBackNow() {
        if (!canRead() || !canWrite()) return // cannot touch contacts now: keep the journal so it is done later
        val journal = journal()
        val markedNow = journal.filter { find(it)?.starred == true }.toSet()
        // Whoever is not marked any more (the user changed it, or the contact is gone) has nothing to give back and leaves the journal.
        val stuck = planReturn(journal, markedNow).filterNot { key -> find(key)?.let { setStarred(it.id, false) } == true }.toSet()
        setJournal(stuck)
        Monitoring.breadcrumb("vip", "restored", "count" to (journal.size - stuck.size))
        if (stuck.isNotEmpty()) Monitoring.nonFatal("vip.still_on_loan", "count" to stuck.size)
        Log.d(TAG, "gave back ${journal.size - stuck.size} of ${journal.size}")
    }

    private class Found(val id: Long, val starred: Boolean)

    private fun find(lookupKey: String): Found? = runCatching {
        val uri = Uri.withAppendedPath(Contacts.CONTENT_LOOKUP_URI, Uri.encode(lookupKey))
        resolver.query(uri, arrayOf(Contacts._ID, Contacts.STARRED), null, null, null)?.use { c ->
            if (c.moveToFirst()) Found(c.getLong(0), c.getInt(1) == 1) else null
        }
    }.getOrNull()

    private fun setStarred(id: Long, on: Boolean): Boolean = runCatching {
        val values = ContentValues().apply { put(Contacts.STARRED, if (on) 1 else 0) }
        resolver.update(Uri.withAppendedPath(Contacts.CONTENT_URI, id.toString()), values, null, null) > 0
    }.getOrDefault(false)

    private fun journal(): Set<String> = journalPrefs.getString(KEY, "").orEmpty().split('\n').filter { it.isNotBlank() }.toSet()

    private fun setJournal(keys: Set<String>) {
        journalPrefs.edit().putString(KEY, keys.joinToString("\n")).commit() // commit: it must be on disk before we touch a contact
        _onLoan.value = keys.size
    }

    private companion object {
        const val TAG = "VipContacts"
        const val KEY = "loaned"
    }
}
