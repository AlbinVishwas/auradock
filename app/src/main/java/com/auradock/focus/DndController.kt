package com.auradock.focus

import android.app.NotificationManager
import android.app.NotificationManager.Policy
import android.content.Context
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** The thin Android layer: reads and writes the phone's Do Not Disturb settings. All decisions are in [planDnd]. */
@Singleton
class DndController @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val manager = context.getSystemService(NotificationManager::class.java)

    /** DND access is a special permission the user grants on a system settings page (not a normal pop-up). */
    fun hasAccess(): Boolean = manager.isNotificationPolicyAccessGranted

    fun snapshot(): DndSnapshot {
        val policy = manager.notificationPolicy
        return DndSnapshot(
            filter = manager.currentInterruptionFilter,
            categories = policy.priorityCategories,
            callSenders = policy.priorityCallSenders,
            messageSenders = policy.priorityMessageSenders,
            conversationSenders = if (Build.VERSION.SDK_INT >= 30) policy.priorityConversationSenders else 0,
            suppressedEffects = policy.suppressedVisualEffects
        )
    }

    /** Priority mode: only alarms and calls from starred contacts can make a sound. */
    fun applyFocus(): Boolean = write {
        manager.notificationPolicy = policy(
            categories = Policy.PRIORITY_CATEGORY_CALLS or Policy.PRIORITY_CATEGORY_ALARMS,
            callSenders = Policy.PRIORITY_SENDERS_STARRED,
            messageSenders = Policy.PRIORITY_SENDERS_STARRED,
            conversationSenders = 3, // Policy.CONVERSATION_SENDERS_NONE
            effects = 0
        )
        manager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
    }

    /** Puts back exactly what [snapshot] holds. */
    fun restore(snapshot: DndSnapshot): Boolean = write {
        manager.notificationPolicy = policy(
            snapshot.categories, snapshot.callSenders, snapshot.messageSenders, snapshot.conversationSenders, snapshot.suppressedEffects
        )
        if (snapshot.filter != 0) manager.setInterruptionFilter(snapshot.filter) // 0 = "unknown": nothing to set
    }

    private fun policy(categories: Int, callSenders: Int, messageSenders: Int, conversationSenders: Int, effects: Int): Policy =
        if (Build.VERSION.SDK_INT >= 30) Policy(categories, callSenders, messageSenders, effects, conversationSenders)
        else Policy(categories, callSenders, messageSenders, effects)

    private fun write(block: () -> Unit): Boolean = try {
        block()
        true
    } catch (e: SecurityException) {
        Log.w(TAG, "no DND access", e) // the user withdrew the permission
        false
    }

    private companion object {
        const val TAG = "DockFocus"
    }
}
