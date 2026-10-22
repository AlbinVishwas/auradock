package com.auradock.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.auradock.core.StandbyController
import com.auradock.service.StandbyForegroundService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

private const val TAG = "AuraDock"

/**
 * Restores standby monitoring after device reboot.
 * WorkManager workers survive reboots automatically; this re-starts the
 * foreground service only if the device is currently charging on boot.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var controller: StandbyController

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        Log.d(TAG, "[BootReceiver] BOOT_COMPLETED received — checking charging state")

        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val isCharging = batteryManager.isCharging
        Log.d(TAG, "[BootReceiver] Battery charging on boot: $isCharging")
        if (isCharging) {
            Log.d(TAG, "[BootReceiver] Device is charging — re-registering receivers and restarting StandbyForegroundService")
            controller.onChargingStarted()
            ContextCompat.startForegroundService(
                context,
                Intent(context, StandbyForegroundService::class.java)
            )
            Log.d(TAG, "[BootReceiver] StandbyForegroundService re-started successfully")
        } else {
            Log.d(TAG, "[BootReceiver] Device is not charging — standby monitoring not started")
        }
    }
}
