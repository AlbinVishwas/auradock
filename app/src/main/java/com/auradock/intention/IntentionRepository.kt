package com.auradock.intention

import android.content.Context
import com.auradock.time.MinuteClock
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** Today's entry once it has been read; [entry] is null when nothing was ever stored for the day. Wrapped so "not read yet" is not "empty". */
data class IntentionToday(val entry: IntentionEntity?)

/** The one place the morning intention is read and changed. Everything stays in the local database. */
@Singleton
class IntentionRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val dao: IntentionDao,
    private val clock: MinuteClock,
    private val events: com.auradock.analytics.EventLog
) {
    private val prefs = context.getSharedPreferences("auradock_prefs", Context.MODE_PRIVATE)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Today's entry; the day rolls over at 4 am by itself. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val today: Flow<IntentionToday> = clock.minutes
        .map { intentionDay(it) }
        .distinctUntilChanged()
        .flatMapLatest { day -> dao.observe(day).map { IntentionToday(it) } }

    private fun today() = intentionDay(clock.currentMillis())

    /** The prompt has been shown: it will not be offered again today, but last evening's suggestion stays as its starting text. */
    fun markOffered() = scope.launch {
        dao.deleteBefore(java.time.LocalDate.parse(today()).minusDays(30).toString())
        val current = dao.get(today())
        if (needsOffer(current)) dao.upsert(IntentionEntity(today(), current?.text.orEmpty(), status = IntentionStatus.OFFERED.name))
    }

    fun save(raw: String) = scope.launch {
        val text = cleanIntention(raw) ?: return@launch skipNow()
        dao.upsert(IntentionEntity(today(), text, status = IntentionStatus.SET.name))
        events.log(com.auradock.analytics.AnalyticsEvents.INTENTION_SET)
    }

    fun skip() = scope.launch { skipNow() }

    private suspend fun skipNow() {
        val current = dao.get(today())
        if (current?.status == IntentionStatus.SET.name) return
        dao.upsert(IntentionEntity(today(), status = IntentionStatus.SKIPPED.name))
    }

    /** Ticks or un-ticks today's intention. */
    fun setDone(done: Boolean) = scope.launch {
        val current = dao.get(today()) ?: return@launch
        if (current.status != IntentionStatus.SET.name) return@launch
        dao.upsert(current.copy(done = done, doneAt = if (done) clock.currentMillis() else null))
        if (done) events.log(com.auradock.analytics.AnalyticsEvents.INTENTION_DONE)
    }

    /** True if the undock card of a session that ended at [endedAt] should ask "Tomorrow's top task?" (once per evening). */
    fun shutdownEligible(endedAt: Long): Boolean = shouldOfferShutdown(endedAt, prefs.getString(KEY_SHUTDOWN_DAY, null))

    /** The question has been shown: not again this evening. */
    fun markShutdownOffered(endedAt: Long) {
        prefs.edit().putString(KEY_SHUTDOWN_DAY, intentionDay(endedAt)).apply()
    }

    /** Tomorrow's top task becomes tomorrow's suggested morning intention (it is only a suggestion until they save it). */
    fun suggestForTomorrow(raw: String) = scope.launch {
        val text = cleanIntention(raw) ?: return@launch
        val day = nextIntentionDay(clock.currentMillis())
        val existing = dao.get(day)
        if (existing == null || existing.status == IntentionStatus.SUGGESTED.name) dao.upsert(IntentionEntity(day, text, status = IntentionStatus.SUGGESTED.name))
    }

    private companion object {
        const val KEY_SHUTDOWN_DAY = "shutdown_offered_day"
    }
}
