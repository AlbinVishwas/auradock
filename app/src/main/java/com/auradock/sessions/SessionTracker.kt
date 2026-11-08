package com.auradock.sessions

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.auradock.data.AuraDockDatabase
import com.auradock.data.RawNotification
import com.auradock.focus.DockSource
import com.auradock.tasks.TaskDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
@Singleton
class SessionTracker @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessions: FocusSessionDao,
    private val tasks: TaskDao
) {
    private val machine = SessionMachine()
    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val seenKeys = mutableSetOf<String>()
    @Volatile private var watcherAskedAt = 0L

    private val _live = MutableStateFlow<LiveSession?>(null)

    /** The session going on right now, or null. The ring and the "held for you" line read this. */
    val live: StateFlow<LiveSession?> = _live.asStateFlow()

    private val _summary = MutableStateFlow<SessionSummary?>(null)

    /** The latest finished session worth showing a card for, until the card is dismissed. */
    val summary: StateFlow<SessionSummary?> = _summary.asStateFlow()

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
        if (machine.live == null && active.isNotEmpty()) seenKeys.clear()
        machine.onShowing(active.isNotEmpty(), source, now)
        _live.value = machine.live
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
    fun onNotification(raw: RawNotification, picked: Boolean) {
        if (machine.live == null || machine.closing) return
        if (!seenKeys.add(raw.key)) return // an update to one already counted
        val arrival = classifyArrival(
            ongoing = raw.ongoing,
            groupSummary = raw.groupSummary,
            empty = raw.title.isBlank() && raw.text.isBlank(),
            own = raw.packageName == context.packageName,
            picked = picked,
            dndActive = raw.dndActive,
            allowedByDnd = raw.allowedByDnd
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
        val done = machine.finishIfDue(System.currentTimeMillis()) ?: return
        _live.value = machine.live
        if (!done.worthKeeping) {
            Log.d(TAG, "session of ${done.durationMs / 1000} s ignored (under two minutes)")
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
            // The card was announced when the dock closed ([announce]); setting it again here would show it twice.
            Log.d(TAG, "session saved: ${done.durationMs / 1000} s, $tasksDone tasks, ${done.counts}")
        }
    }

    private companion object {
        const val TAG = "SessionTracker"
        const val WATCHER_WINDOW_MS = 15_000L
    }
}
