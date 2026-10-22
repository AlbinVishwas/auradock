package com.auradock.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.auradock.core.StandbyController
import com.auradock.service.StandbyForegroundService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

private const val TAG = "AuraDock"

/**
 * Listens for charger plug/unplug events.
 *
 * Intentionally manages [StandbyForegroundService] lifecycle here (not in
 * [StandbyController]) to avoid a circular dependency:
 *   Service → OrientationDetector → StandbyController → Service
 */
@AndroidEntryPoint
class PowerReceiver : BroadcastReceiver() {

    @Inject
    lateinit var controller: StandbyController

    override fun onReceive(context: Context, intent: Intent) {
        val serviceIntent = Intent(context, StandbyForegroundService::class.java)
        when (intent.action) {
            Intent.ACTION_POWER_CONNECTED -> {
                Log.d(TAG, "[PowerReceiver] ACTION_POWER_CONNECTED received — charger plugged in")
                controller.onChargingStarted()
                Log.d(TAG, "[PowerReceiver] Starting StandbyForegroundService for orientation tracking")
                ContextCompat.startForegroundService(context, serviceIntent)
            }
            Intent.ACTION_POWER_DISCONNECTED -> {
                Log.d(TAG, "[PowerReceiver] ACTION_POWER_DISCONNECTED received — charger unplugged")
                controller.onChargingStopped()
                Log.d(TAG, "[PowerReceiver] Stopping StandbyForegroundService")
                context.stopService(serviceIntent)
            }
        }
    }
}
