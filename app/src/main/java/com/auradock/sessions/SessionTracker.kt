package com.auradock.sessions

import com.auradock.monitoring.Monitoring
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import com.auradock.data.AuraDockDatabase
import com.auradock.data.RawNotification
import com.auradock.focus.DockSource
import com.auradock.tasks.TaskDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** What the undock card shows: how long, how many tasks were ticked, how many notifications were held and let through. */
data class SessionSummary(val endedAt: Long, val durationMs: Long, val tasksCompleted: Int, val counts: SessionCounts)

/**
 * The thin Android layer around [SessionMachine]: it is told when a dock opens and closes ([onDockSources]) and when a
 * notification arrives ([onNotification]), keeps the live session for the dock to draw, and saves a finished one that is
 * long enough. All the rules are in [SessionMachine], [classifyArrival] and [FinishedSession.worthKeeping].
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class SessionTracker @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessions: FocusSessionDao,
    private val tasks: TaskDao,
    private val events: com.auradock.analytics.EventLog
) {
    private val machine = SessionMachine()
    private val handler = Handler(Looper.getMainLooper())
    // One thread for everything that touches the database, so a recovery always finishes before the next checkpoint is written.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))
    private val seenKeys = mutableSetOf<String>()
    @Volatile private var watcherAskedAt = 0L

    private val _live = MutableStateFlow<LiveSession?>(null)

    /** The session going on right now, or null. The ring and the "held for you" line read this. */
    val live: StateFlow<LiveSession?> = _live.asStateFlow()

    private val _summary = MutableStateFlow<SessionSummary?>(null)

    /** The latest finished session worth showing a card for, until the card is dismissed. */
    val summary: StateFlow<SessionSummary?> = _summary.asStateFlow()

    /**
     * Call once at app start. If the app was killed while docked, the open session's last checkpoint is closed as a finished
     * session (kept if it was two minutes or more) and cleared.
     */
    fun recoverOpenSession() {
        scope.launch {
            val checkpoint = sessions.checkpoint() ?: return@launch
            val done = recoveredSession(checkpoint)
            Monitoring.breadcrumb("focus_session", "checkpoint_recovered", "kept" to done.worthKeeping)
            Monitoring.nonFatal("session.closed_from_checkpoint", "kept" to done.worthKeeping)
            if (done.worthKeeping) {
                val tasksDone = tasks.countCompletedBetween(done.startedAt, done.endedAt)
                sessions.insert(
                    FocusSessionEntity(
                        startedAt = done.startedAt, endedAt = done.endedAt, source = done.source.name,
                        notificationsHeld = done.counts.held, vipThrough = done.counts.through, tasksCompleted = tasksDone
                    )
                )
                Log.i(TAG, "recovered an open session: ${done.durationMs / 1000} s, ${done.counts}")
            }
            sessions.clearCheckpoint()
        }
    }

    /** Writes the open session down (start time and latest counts). [at] is the last moment it was known to be open. */
    private fun writeCheckpoint(session: LiveSession, at: Long) {
        Monitoring.breadcrumb("focus_session", "checkpoint")
        scope.launch {
            sessions.saveCheckpoint(
                SessionCheckpointEntity(
                    startedAt = session.startedAt, lastAt = at, source = session.source.name,
                    notificationsHeld = session.counts.held, vipThrough = session.counts.through
                )
            )
        }
    }

    private val tick = object : Runnable {
        override fun run() {
            synchronized(this@SessionTracker) {
                val session = machine.live
                if (session != null && !machine.closing) writeCheckpoint(session, System.currentTimeMillis())
            }
            handler.postDelayed(this, CHECKPOINT_INTERVAL_MS)
        }
    }

    fun dismissSummary() {
        _summary.value = null
    }

    /** The "Dock from anywhere" service is about to open the dock, so the session that starts is a watcher session. */
    fun noteWatcherLaunch() {
        watcherAskedAt = System.currentTimeMillis()
    }

    /** The docks that are showing now. Called by DockFocusController whenever one opens or closes. */
    @Synchronized
    fun onDockSources(active: Set<DockSource>) {
        val now = System.currentTimeMillis()
        val source = when {
            DockSource.DREAM in active -> SessionSource.SCREENSAVER
            now - watcherAskedAt < WATCHER_WINDOW_MS -> SessionSource.WATCHER
            else -> SessionSource.DOCK_NOW
        }
        val starting = machine.live == null && active.isNotEmpty()
        if (starting) {
            seenKeys.clear()
            Monitoring.breadcrumb("focus_session", "start", "source" to source.name)
            events.log(com.auradock.analytics.AnalyticsEvents.SESSION_START, source.name)
            events.logOnce(com.auradock.analytics.AnalyticsEvents.FIRST_DOCK, source.name)
        }
        machine.onShowing(active.isNotEmpty(), source, now, SystemClock.elapsedRealtime())
        _live.value = machine.live
        if (starting) {
            machine.live?.let { writeCheckpoint(it, now) }
            handler.removeCallbacks(tick)
            handler.postDelayed(tick, CHECKPOINT_INTERVAL_MS)
        }
        if (active.isEmpty()) machine.provisional()?.let { writeCheckpoint(LiveSession(it.startedAt, it.source, it.counts), it.endedAt) }
        if (active.isNotEmpty()) {
            _summary.value = null // a dock is back (or a new session): the old card is no longer news
        } else {
            // The dock has just closed: the card should not wait out the handover grace. If a dock comes back it is withdrawn.
            machine.provisional()?.takeIf { it.worthKeeping }?.let { announce(it) }
        }
        handler.removeCallbacks(finish)
        if (active.isEmpty()) handler.postDelayed(finish, HANDOVER_GRACE_MS + 100)
    }

    /** A notification came in. [picked] is true if it is from one of the user's picked apps. */
    @Synchronized
    fun onNotification(raw: RawNotification, picked: Boolean, vip: Boolean = false) {
        if (machine.live == null || machine.closing) return
        if (!seenKeys.add(raw.key)) return // an update to one already counted
        val arrival = classifyArrival(
            ongoing = raw.ongoing,
            groupSummary = raw.groupSummary,
            empty = raw.title.isBlank() && raw.text.isBlank(),
            own = raw.packageName == context.packageName,
            picked = picked,
            dndActive = raw.dndActive,
            allowedByDnd = raw.allowedByDnd,
            vip = vip
        )
        if (arrival == Arrival.IGNORED) seenKeys.remove(raw.key)
        machine.count(arrival)
        _live.value = machine.live
    }

    /** A notification went away: if the same one comes back it is a new one and counts again. */
    @Synchronized
    fun onNotificationRemoved(key: String) {
        seenKeys.remove(key)
    }

    private fun announce(done: FinishedSession) {
        scope.launch {
            val tasksDone = tasks.countCompletedBetween(done.startedAt, done.endedAt)
            synchronized(this@SessionTracker) {
                if (machine.closing) _summary.value = SessionSummary(done.endedAt, done.durationMs, tasksDone, done.counts)
            }
        }
    }

    private val finish = Runnable { finishNow() }

    @Synchronized
    private fun finishNow() {
        val done = machine.finishIfDue(System.currentTimeMillis(), SystemClock.elapsedRealtime()) ?: return
        Monitoring.breadcrumb("focus_session", "end", "kept" to done.worthKeeping)
        events.log(com.auradock.analytics.AnalyticsEvents.SESSION_END, (done.durationMs / 1000).toString())
        _live.value = machine.live
        handler.removeCallbacks(tick)
        if (!done.worthKeeping) {
            Log.i(TAG, "session of ${done.durationMs / 1000} s ignored (under two minutes)")
            scope.launch { sessions.clearCheckpoint() }
            return
        }
        scope.launch {
            val tasksDone = tasks.countCompletedBetween(done.startedAt, done.endedAt)
            sessions.insert(
                FocusSessionEntity(
                    startedAt = done.startedAt,
                    endedAt = done.endedAt,
                    source = done.source.name,
                    notificationsHeld = done.counts.held,
                    vipThrough = done.counts.through,
                    tasksCompleted = tasksDone
                )
            )
            sessions.clearCheckpoint()
            // The card was announced when the dock closed ([announce]); setting it again here would show it twice.
            Log.i(TAG, "session saved: ${done.durationMs / 1000} s, $tasksDone tasks, ${done.counts}")
        }
    }

    private companion object {
        const val TAG = "SessionTracker"
        const val WATCHER_WINDOW_MS = 15_000L
    }
}
