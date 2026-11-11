package com.auradock.watch

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.auradock.focus.FocusSettings
import com.auradock.permissions.canDrawOverOtherApps
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Starts or stops [DockWatcherService] so that it runs exactly when "Dock from anywhere" is on and "Display over other apps"
 * is allowed. Call [sync] whenever either might have changed: the switch, the app opening, the phone booting.
 */
@Singleton
class DockWatcherController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: FocusSettings
) {
    fun shouldRun(): Boolean =
        settings.dockFromAnywhere.value && canDrawOverOtherApps(context) &&
            com.auradock.analytics.FeatureFlags.isOn(com.auradock.analytics.FeatureFlags.DOCK_FROM_ANYWHERE) // can be switched off remotely

    fun sync() {
        val intent = Intent(context, DockWatcherService::class.java)
        if (shouldRun()) {
            // Android 12+ refuses to start a foreground service from the background in some situations; then it simply
            // starts the next time the app opens.
            runCatching { ContextCompat.startForegroundService(context, intent) }
                .onFailure { Log.w(TAG, "Could not start the dock watcher now", it) }
        } else {
            context.stopService(intent)
        }
    }

    private companion object {
        const val TAG = "DockWatcher"
    }
}
