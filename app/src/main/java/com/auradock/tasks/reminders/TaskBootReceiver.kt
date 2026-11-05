package com.auradock.tasks.reminders

import android.content.Context
import android.content.Intent
import com.auradock.focus.DockFocusController
import com.auradock.watch.DockWatcherController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Sets every pending alarm again after something wiped them:
 *  - the phone restarted (BOOT_COMPLETED): Android forgets all alarms on shutdown;
 *  - AuraDock was updated (MY_PACKAGE_REPLACED);
 *  - the user just allowed exact alarms (SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED): upgrade inexact alarms.
 */
@AndroidEntryPoint
class TaskBootReceiver : HiltBroadcastReceiver() {
    @Inject lateinit var scheduler: TaskAlarmScheduler
    @Inject lateinit var dockFocus: DockFocusController
    @Inject lateinit var dockWatcher: DockWatcherController

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                scheduler.rescheduleAll()
                dockWatcher.sync() // "Dock from anywhere" starts again after a restart or an update
                dockFocus.refresh() // after a restart no dock is showing: undo any Do Not Disturb we had switched on
            } finally {
                pending.finish()
            }
        }
    }
}
