package com.auradock.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.auradock.R
import com.auradock.sensor.OrientationDetector
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Long-running foreground service that keeps [OrientationDetector] alive
 * while the phone is charging (CHARGING or STANDBY_ACTIVE states).
 *
 * Lifecycle managed by [com.auradock.receiver.PowerReceiver]:
 *   started on ACTION_POWER_CONNECTED, stopped on ACTION_POWER_DISCONNECTED.
 *
 * START_STICKY ensures the OS restarts this service if it is killed,
 * required for reliable dock detection.
 */
@AndroidEntryPoint
class StandbyForegroundService : Service() {

    @Inject
    lateinit var orientationDetector: OrientationDetector

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "[StandbyForegroundService] Service STARTED — building foreground notification")
        startForeground(NOTIF_ID, buildNotification())
        Log.d(TAG, "[StandbyForegroundService] Launching orientation observation coroutine")
        serviceScope.launch {
            Log.d(TAG, "[StandbyForegroundService] Orientation observation active — waiting for sensor events")
            orientationDetector.startObserving()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        serviceScope.cancel()
        Log.d(TAG, "[StandbyForegroundService] Service STOPPED — coroutine scope cancelled, orientation tracking halted")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID,
                    getString(R.string.notif_channel_name),
                    NotificationManager.IMPORTANCE_LOW)
            )
        }
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notif_content_title))
            .setContentText(getString(R.string.notif_content_text))
            .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "auradock_standby_channel"
        const val NOTIF_ID   = 1001
        private const val TAG = "AuraDock"
    }
}
