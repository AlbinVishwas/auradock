package com.auradock.focus

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.auradock.focus.vip.VipContacts
import com.auradock.sessions.SessionTracker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Something that can show the dock. DND stays on while at least one of them is showing. */
enum class DockSource { APP, DREAM }

/**
 * Turns Do Not Disturb on while the dock is showing and puts it back afterwards, following [planDnd].
 *
 * The saved state is written to disk *before* DND is changed, so even if the app is killed while docked, the next
 * launch, a reboot, or the end of the screensaver ([refresh] with no dock showing) puts the user's settings back.
 */
@Singleton
class DockFocusController @Inject constructor(
    @ApplicationContext context: Context,
    private val settings: FocusSettings,
    private val dnd: DndController,
    private val sessions: SessionTracker,
    private val vipContacts: VipContacts
) {
    private val prefs = context.getSharedPreferences("auradock_focus_state", Context.MODE_PRIVATE)
    private val active = mutableSetOf<DockSource>()
    private val _showing = MutableStateFlow(false)

    /** True while any dock (in the app or the screensaver) is on screen. Changes the moment a dock opens or closes. */
    val showing: StateFlow<Boolean> = _showing.asStateFlow()
    private val handler = Handler(Looper.getMainLooper())
    private var restorePending = false
    private var undone = false // "Undo focus" was used: no Focus for the rest of this dock session
    private val _focusOn = MutableStateFlow(false)

    /** True while we have Focus (Do Not Disturb) switched on for the dock. The dock offers "Undo focus" only then. */
    val focusOn: StateFlow<Boolean> = _focusOn.asStateFlow()

    private val delayedRefresh = Runnable {
        restorePending = false
        undone = false // the dock session is over for good (past the handover grace)
        refresh()
    }

    /**
     * "Undo focus": puts Do Not Disturb (and any loaned Favourite marks) back to the user's own state now, without ending the dock. Focus
     * stays off until the dock session ends.
     */
    @Synchronized
    fun undoFocus() {
        undone = true
        refresh()
    }

    /**
     * A dock appeared ([isActive] true) or went away.
     *
     * Going away waits a few seconds before putting DND back: turning the phone, or the screensaver handing over to the
     * app, closes one dock and opens the next within a moment, and DND should not flicker off and on in between. If a dock
     * comes back inside that window the pending restore is simply cancelled.
     */
    @Synchronized
    fun setActive(source: DockSource, isActive: Boolean) {
        if (isActive) active += source else active -= source
        _showing.value = active.isNotEmpty()
        sessions.onDockSources(active.toSet())
        handler.removeCallbacks(delayedRefresh)
        restorePending = false
        if (!isActive && active.isEmpty()) {
            restorePending = true
            handler.postDelayed(delayedRefresh, RESTORE_GRACE_MS)
        } else {
            refresh()
        }
    }

    /**
     * Like [refresh], but if a dock has only just closed and its delayed restore is still waiting, leave that to the timer.
     * Used for "the screen came back" checks (for example after the Activity is recreated by a rotation).
     */
    @Synchronized
    fun refreshRespectingGrace() {
        if (!restorePending) refresh()
    }

    /**
     * Re-checks everything and acts if needed. Call it whenever something relevant may have changed: a dock showing or
     * closing, the Focus switch, DND access granted, the app starting, the phone booting.
     */
    @Synchronized
    fun refresh() {
        val access = dnd.hasAccess()
        val state = load()
        val want = settings.focusWhileDocked.value && access && active.isNotEmpty() && !undone
        // Without DND access we cannot change anything, so a pending restore would fail too: keep waiting for access.
        if (!access && state.changed) {
            vipContacts.giveBack()
            return
        }

        when (val action = planDnd(want, state, dnd.snapshot())) {
            DndAction.None -> Unit
            is DndAction.Apply -> {
                save(DockDndState(changed = true, saved = action.save, applied = null)) // remember first...
                if (dnd.applyFocus()) {
                    save(DockDndState(changed = true, saved = action.save, applied = dnd.snapshot())) // ...then note what we set
                    Log.d(TAG, "Focus on (saved ${action.save})")
                } else {
                    clear()
                }
            }
            is DndAction.Restore -> {
                dnd.restore(action.snapshot)
                clear()
                Log.d(TAG, "Focus off, restored ${action.snapshot}")
            }
            DndAction.Forget -> {
                clear()
                Log.d(TAG, "DND was changed by the user while docked: left alone")
            }
        }
        // People who can always reach the user ring through only while our focus is on; the moment it is not, they are put back.
        val ours = load().changed
        _focusOn.value = want && ours
        if (want && ours) vipContacts.lend() else vipContacts.giveBack()
    }

    private fun load(): DockDndState {
        if (!prefs.getBoolean("changed", false)) return DockDndState()
        return DockDndState(changed = true, saved = readSnapshot("saved"), applied = readSnapshot("applied"))
    }

    private fun save(state: DockDndState) {
        prefs.edit().apply {
            putBoolean("changed", state.changed)
            writeSnapshot("saved", state.saved)
            writeSnapshot("applied", state.applied)
        }.commit() // commit (not apply): it must be on disk before we touch DND
    }

    private fun clear() {
        prefs.edit().clear().commit()
    }

    private fun readSnapshot(prefix: String): DndSnapshot? {
        if (!prefs.contains("$prefix.filter")) return null
        return DndSnapshot(
            filter = prefs.getInt("$prefix.filter", 0),
            categories = prefs.getInt("$prefix.categories", 0),
            callSenders = prefs.getInt("$prefix.callSenders", 0),
            messageSenders = prefs.getInt("$prefix.messageSenders", 0),
            conversationSenders = prefs.getInt("$prefix.conversationSenders", 0),
            suppressedEffects = prefs.getInt("$prefix.suppressedEffects", 0)
        )
    }

    private fun android.content.SharedPreferences.Editor.writeSnapshot(prefix: String, s: DndSnapshot?) {
        if (s == null) {
            remove("$prefix.filter")
            return
        }
        putInt("$prefix.filter", s.filter)
        putInt("$prefix.categories", s.categories)
        putInt("$prefix.callSenders", s.callSenders)
        putInt("$prefix.messageSenders", s.messageSenders)
        putInt("$prefix.conversationSenders", s.conversationSenders)
        putInt("$prefix.suppressedEffects", s.suppressedEffects)
    }

    private companion object {
        const val TAG = "DockFocus"
        const val RESTORE_GRACE_MS = 3_000L
    }
}
