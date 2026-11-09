package com.auradock.intention

import java.time.Instant
import java.time.ZoneId

/** A new day for the intention starts at 4 am: someone docking at 1 am is still on yesterday's day. */
const val INTENTION_DAY_STARTS_HOUR = 4

/** Longest intention kept. The dock shows one quiet line, so a long one is cut. */
const val INTENTION_MAX_CHARS = 80

/** The day an instant belongs to, as "yyyy-MM-dd", counting the day from [INTENTION_DAY_STARTS_HOUR]. */
fun intentionDay(millis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
    Instant.ofEpochMilli(millis).atZone(zone).minusHours(INTENTION_DAY_STARTS_HOUR.toLong()).toLocalDate().toString()

/** The day after [intentionDay]. */
fun nextIntentionDay(millis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
    Instant.ofEpochMilli(millis).atZone(zone).minusHours(INTENTION_DAY_STARTS_HOUR.toLong()).toLocalDate().plusDays(1).toString()

/** Trims, joins lines and cuts the text. Null if nothing is left. */
fun cleanIntention(raw: String): String? =
    raw.trim().replace(Regex("\\s+"), " ").take(INTENTION_MAX_CHARS).trim().ifEmpty { null }

/** True if the dock should ask today's question: it was never asked, or only last evening's suggestion is waiting. */
fun needsOffer(entry: IntentionEntity?): Boolean = entry == null || entry.status == IntentionStatus.SUGGESTED.name

/** What the dock and the dashboard show for a day's entry: null unless the user wrote one. */
fun intentionLine(entry: IntentionEntity?): IntentionLine? =
    entry?.takeIf { it.status == IntentionStatus.SET.name && it.text.isNotBlank() }?.let { IntentionLine(it.text, it.done) }

/** The one thing for today, as a quiet line with a circle: [done] ticks it. */
data class IntentionLine(val text: String, val done: Boolean)
