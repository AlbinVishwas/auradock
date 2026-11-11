package com.auradock.sessions

/** How a focus session started. */
enum class SessionSource {
    /** The screensaver (DreamService) started the dock. */
    SCREENSAVER,

    /** The user pressed "Dock now", or opened the app while charging sideways and the dock showed itself. */
    DOCK_NOW,

    /** The "Dock from anywhere" watcher opened the dock. */
    WATCHER
}

/** A session shorter than this is not worth keeping: a phone briefly turned sideways is not focus. */
const val MIN_SESSION_MS = 2 * 60_000L

/**
 * One dock closing and the next opening (the screensaver handing over to the app, or a turn of the phone) happens within a
 * moment. Inside this window the two are one session. It is longer than the Do Not Disturb restore grace in DockFocusController
 * (3 s) on purpose: recreating the dock Activity can take several seconds on a busy phone, and a session split in two by that
 * would be dropped when each half is under two minutes.
 */
const val HANDOVER_GRACE_MS = 10_000L

/** What came in while docked: [held] were silenced, [through] reached the user (a picked app, or someone Do Not Disturb lets through). */
data class SessionCounts(val held: Int = 0, val through: Int = 0)

/** A session that is going on now. */
data class LiveSession(
    val startedAt: Long,
    val source: SessionSource,
    val counts: SessionCounts = SessionCounts(),
    /** The monotonic clock reading when it started: lengths come from this, never from the wall clock, which can be stepped. */
    val startElapsed: Long = startedAt
)

/** A session that has ended. */
data class FinishedSession(
    val startedAt: Long,
    val endedAt: Long,
    val source: SessionSource,
    val counts: SessionCounts
) {
    val durationMs: Long get() = endedAt - startedAt

    /** Sessions under two minutes are ignored: not saved, no summary. */
    val worthKeeping: Boolean get() = durationMs >= MIN_SESSION_MS
}

/** How a notification that arrived during a session is counted. */
enum class Arrival { HELD, THROUGH, IGNORED }

/**
 * Decides how one incoming notification counts. [picked]: from one of the user's picked apps (the dock shows it).
 * [dndActive]: Do Not Disturb is filtering right now. [allowedByDnd]: Android would let this one through the filter (a starred
 * contact, say). [vip]: it is from one of the people who can always reach the user. Nothing is counted for AuraDock's own notifications, ongoing ones, group headers or empty ones.
 */
fun classifyArrival(
    ongoing: Boolean,
    groupSummary: Boolean,
    empty: Boolean,
    own: Boolean,
    picked: Boolean,
    dndActive: Boolean,
    allowedByDnd: Boolean,
    vip: Boolean = false
): Arrival = when {
    ongoing || groupSummary || empty || own -> Arrival.IGNORED
    picked || vip -> Arrival.THROUGH
    !dndActive -> Arrival.IGNORED // nothing was silenced, so there is nothing held to report
    allowedByDnd -> Arrival.THROUGH
    else -> Arrival.HELD
}

/**
 * The start-and-end rules, with no Android in them. Feed it "a dock is showing / is not showing" and the time; it keeps one
 * live session, merges a handover into it, and hands back the finished session once the grace has passed.
 */
class SessionMachine {
    var live: LiveSession? = null
        private set
    private var closedAt: Long? = null
    private var closedElapsed: Long = 0L

    /** True while a dock has just closed and the grace is running. */
    val closing: Boolean get() = closedAt != null

    fun onShowing(showing: Boolean, source: SessionSource, now: Long, elapsed: Long = now) {
        if (showing) {
            if (live == null) live = LiveSession(startedAt = now, source = source, startElapsed = elapsed)
            closedAt = null // a handover: the same session carries on
        } else if (live != null && closedAt == null) {
            closedAt = now
            closedElapsed = elapsed
        }
    }

    /** Counts a notification, but only while a dock is really showing. */
    fun count(arrival: Arrival) {
        val session = live ?: return
        if (closedAt != null) return
        live = session.copy(
            counts = when (arrival) {
                Arrival.HELD -> session.counts.copy(held = session.counts.held + 1)
                Arrival.THROUGH -> session.counts.copy(through = session.counts.through + 1)
                Arrival.IGNORED -> session.counts
            }
        )
    }

    /** The session as it stands now that its dock has closed, for the undock card, which cannot wait out the grace. */
    fun provisional(): FinishedSession? {
        val session = live ?: return null
        closedAt ?: return null
        return finished(session)
    }

    /** The session as it ended: its end is its start plus the monotonic time it ran, so a stepped wall clock cannot change its length. */
    private fun finished(session: LiveSession) =
        FinishedSession(session.startedAt, session.startedAt + (closedElapsed - session.startElapsed).coerceAtLeast(0L), session.source, session.counts)

    /** Call once the grace may have passed. Returns the session if it has now really ended. */
    fun finishIfDue(now: Long, elapsed: Long = now): FinishedSession? {
        val session = live ?: return null
        closedAt ?: return null
        if (elapsed - closedElapsed < HANDOVER_GRACE_MS) return null
        val done = finished(session)
        live = null
        closedAt = null
        return done
    }
}

/** How often the open session is written down, so a killed app loses at most this much. */
const val CHECKPOINT_INTERVAL_MS = 60_000L

/** A session left open by a killed app, closed at its last checkpoint. Whether it is long enough to keep is [FinishedSession.worthKeeping]. */
fun recoveredSession(checkpoint: SessionCheckpointEntity): FinishedSession = FinishedSession(
    startedAt = checkpoint.startedAt,
    endedAt = maxOf(checkpoint.startedAt, checkpoint.lastAt),
    source = SessionSource.entries.firstOrNull { it.name == checkpoint.source } ?: SessionSource.DOCK_NOW,
    counts = SessionCounts(checkpoint.notificationsHeld, checkpoint.vipThrough)
)
