package com.auradock.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.auradock.widget.WidgetManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Periodic WorkManager task that triggers data refresh for all registered widgets
 * that rely on background fetch (e.g. [com.auradock.widget.impl.WeatherWidget]).
 *
 * Survives process death and device reboots. Respects WorkManager's battery
 * optimisation policies (Doze, App Standby).
 */
@HiltWorker
class WidgetUpdateWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val widgetManager: WidgetManager
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val widgets = widgetManager.getRegisteredWidgets().filter { it.updateIntervalMs > 0 }
        Log.d(TAG, "[WidgetUpdateWorker] Worker STARTED — checking ${widgetManager.getRegisteredWidgets().size} registered widget(s), ${widgets.size} require background refresh")
        return try {
            widgets.forEach { widget ->
                Log.d(TAG, "[WidgetUpdateWorker] Updating widget: ${widget.displayName} (interval: ${widget.updateIntervalMs / 1000}s)")
                widget.onActivate()
            }
            Log.d(TAG, "[WidgetUpdateWorker] Worker COMPLETED successfully — all widgets refreshed")
            Result.success()
        } catch (e: Exception) {
            Log.d(TAG, "[WidgetUpdateWorker] Worker FAILED: ${e.message} — scheduling retry")
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "AuraDock"
    }
}
