package com.auradock.worker

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules and cancels periodic widget background updates via [WorkManager].
 *
 * Called by [com.auradock.core.StandbyController] on STANDBY_ACTIVE entry/exit.
 * Uses [ExistingPeriodicWorkPolicy.KEEP] to avoid duplicate scheduling.
 */
@Singleton
class WorkManagerScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val workManager = WorkManager.getInstance(context)

    fun scheduleWidgetUpdates() {
        val request = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(15, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(false)  // Dock is always plugged in
                    .build()
            )
            .addTag(WIDGET_UPDATE_TAG)
            .build()

        workManager.enqueueUniquePeriodicWork(
            WIDGET_UPDATE_TAG,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
        Log.d(TAG, "[WorkManagerScheduler] Background work scheduled — tag: $WIDGET_UPDATE_TAG, interval: 15 minutes, policy: KEEP")
    }

    fun cancelWidgetUpdates() {
        workManager.cancelAllWorkByTag(WIDGET_UPDATE_TAG)
        Log.d(TAG, "[WorkManagerScheduler] Background work cancelled — tag: $WIDGET_UPDATE_TAG")
    }

    companion object {
        const val WIDGET_UPDATE_TAG = "auradock_widget_update"
        private const val TAG = "AuraDock"
    }
}
