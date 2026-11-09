package com.auradock.data

/** What the notification listener hands us for one notification, as plain values (so the rules can be unit-tested). */
data class RawNotification(
    val key: String,
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val postedAt: Long,
    val ongoing: Boolean,
    val groupSummary: Boolean,
    /** Do Not Disturb is filtering right now (not "allow all"). Only the focus session counts read this. */
    val dndActive: Boolean = false,
    /** Android would let this one through the current Do Not Disturb filter (a starred contact, say). */
    val allowedByDnd: Boolean = false,
    /** Contact links (`content://com.android.contacts/contacts/lookup/<key>/<id>`) the sending app named as the sender. Never stored. */
    val personUris: List<String> = emptyList()
)

/** A notification as the dock shows it. [key] is Android's unique id for it, used to update or remove it exactly. */
data class NotificationItem(
    val key: String,
    val packageName: String,
    val appName: String,
    val title: String,
    val content: String,
    val postedAt: Long
)

const val MAX_CACHED_NOTIFICATIONS = 20

/**
 * Only notifications from the apps the user picked, and only real ones: not "ongoing" ones (a music player, a download
 * progress bar) and not the empty "N new messages" group header Android adds on top of a stack.
 */
fun isRelevant(raw: RawNotification, pickedApps: Set<String>): Boolean =
    raw.packageName in pickedApps && !raw.ongoing && !raw.groupSummary && (raw.title.isNotBlank() || raw.text.isNotBlank())

fun RawNotification.toItem(): NotificationItem =
    NotificationItem(key, packageName, appName, title.ifBlank { appName }, text, postedAt)

/** Adds a notification, or replaces the one with the same key (an update), newest first, keeping at most [max]. */
fun upsert(items: List<NotificationItem>, item: NotificationItem, max: Int = MAX_CACHED_NOTIFICATIONS): List<NotificationItem> =
    (listOf(item) + items.filterNot { it.key == item.key }).sortedByDescending { it.postedAt }.take(max)

/** Forgets a notification the user dismissed (or the app withdrew). */
fun removeByKey(items: List<NotificationItem>, key: String): List<NotificationItem> = items.filterNot { it.key == key }

/** After the user changes their apps: drop what belongs to apps no longer picked. */
fun retainPicked(items: List<NotificationItem>, pickedApps: Set<String>): List<NotificationItem> =
    items.filter { it.packageName in pickedApps }

/** The newest notification from each picked app, newest first: what the dock strip shows. */
fun latestPerApp(items: List<NotificationItem>, pickedApps: List<String>): List<NotificationItem> =
    pickedApps.mapNotNull { pkg -> items.filter { it.packageName == pkg }.maxByOrNull { it.postedAt } }
        .sortedByDescending { it.postedAt }
