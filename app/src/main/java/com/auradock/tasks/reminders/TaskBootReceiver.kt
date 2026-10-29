package com.auradock.tasks.reminders

import android.content.Context
import android.content.Intent
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

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                scheduler.rescheduleAll()
            } finally {
                pending.finish()
            }
        }
    }
}
