package com.auradock.watch

import com.auradock.monitoring.Monitoring
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import com.auradock.MainActivity
import com.auradock.R
import com.auradock.dock.DockWatchLogic
import com.auradock.dock.Pill
import com.auradock.dock.WatchContext
import com.auradock.dock.WatchOutput
import com.auradock.focus.DockFocusController
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * "Dock from anywhere": while the screen is on, watches the accelerometer and, following [DockWatchLogic], offers or
 * starts the dock when the phone is set sideways and still on a stand.
 *
 * It is a foreground service (so Android lets it read the sensor in the background) and shows one quiet notification
 * for as long as it runs. It only reads the sensor while the screen is on: with the screen off the screensaver
 * does the job. The readings are never stored or sent anywhere.
 */
@AndroidEntryPoint
class DockWatcherService : Service(), LifecycleOwner, SavedStateRegistryOwner {

    @Inject lateinit var controller: DockWatcherController
    @Inject lateinit var dockFocus: DockFocusController

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    private lateinit var overlay: PillOverlay
    private val handler = Handler(Looper.getMainLooper())
    private val logic = DockWatchLogic()
    private var sampling = false
    private var charging = false
    private var lastPill: Pill? = null

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            logic.onSample(SystemClock.elapsedRealtime(), event.values[0], event.values[1], event.values[2])
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    private val tick = object : Runnable {
        override fun run() {
            apply(logic.step(SystemClock.elapsedRealtime(), currentContext()))
            handler.postDelayed(this, TICK_MS)
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_ON -> startSampling()
                Intent.ACTION_SCREEN_OFF -> stopSampling()
                Intent.ACTION_BATTERY_CHANGED -> charging = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        savedStateController.performAttach()
        savedStateController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        overlay = PillOverlay(
            context = this,
            owner = this,
            onTap = { apply(logic.onPillTapped()) },
            onCancel = {
                logic.onCancel()
                apply(WatchOutput(null))
            }
        )
        goForeground()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_BATTERY_CHANGED)
        }
        // The battery broadcast is sticky: registering returns the current state straight away.
        registerReceiver(receiver, filter)?.let { charging = it.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0 }
        if (getSystemService(PowerManager::class.java).isInteractive) startSampling()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!controller.shouldRun()) { // switched off, or the permission was taken away, while we were not running
            stopSelf()
            return START_NOT_STICKY
        }
        goForeground() // every start request must be answered with a foreground notification
        return START_STICKY
    }

    override fun onDestroy() {
        stopSampling()
        overlay.release()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        runCatching { unregisterReceiver(receiver) }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startSampling() {
        if (sampling) return
        val sensorManager = getSystemService(SensorManager::class.java)
        val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return
        sampling = true
        logic.reset()
        sensorManager.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        handler.post(tick)
        Log.d(TAG, "watching (screen on, charging=$charging)")
    }

    private fun stopSampling() {
        if (!sampling) return
        sampling = false
        getSystemService(SensorManager::class.java)?.unregisterListener(sensorListener)
        handler.removeCallbacks(tick)
        apply(WatchOutput(null))
        Log.d(TAG, "not watching (screen off)")
    }

    private fun currentContext(): WatchContext {
        val audio = getSystemService(AudioManager::class.java)
        val media = runCatching { audio.activePlaybackConfigurations.isNotEmpty() }.getOrDefault(false)
        val inCall = audio.mode == AudioManager.MODE_IN_CALL || audio.mode == AudioManager.MODE_IN_COMMUNICATION ||
            audio.mode == AudioManager.MODE_RINGTONE
        return WatchContext(charging = charging, mediaPlaying = media, inCall = inCall, dockShowing = dockFocus.showing.value)
    }

    private fun apply(output: WatchOutput) {
        if (output.pill != lastPill) {
            lastPill = output.pill
            Log.d(TAG, "pill: ${output.pill ?: "hidden"}")
            Monitoring.breadcrumb("watcher", if (output.pill == null) "suppressed" else "pill", "reason" to (output.pill?.javaClass?.simpleName ?: "hidden"))
            showPill(output.pill)
        }
        if (output.dockNow) launchDock()
    }

    /** Shows [pill] (null hides it). */
    private fun showPill(pill: Pill?) {
        if (pill == null) overlay.hide() else overlay.show(pill)
    }

    private fun launchDock() {
        Log.d(TAG, "opening the dock")
        Monitoring.breadcrumb("watcher", "trigger")
        startActivity(
            Intent(this, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_DOCK_NOW, true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        )
    }

    private fun goForeground() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Dock from anywhere", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Shown while AuraDock is watching for the phone being set on a stand."
                setShowBadge(false)
            }
        )
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_auradock)
            .setContentTitle("AuraDock is ready to dock")
            .setContentText("Tap to open. Turn off in Focus settings.")
            .setContentIntent(open)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private companion object {
        const val TAG = "DockWatcher"
        const val CHANNEL_ID = "dock_watcher"
        const val NOTIFICATION_ID = 4101
        const val TICK_MS = 250L
    }
}
