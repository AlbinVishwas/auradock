package com.auradock.permissions

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

fun openNotificationAccessSettings(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    )
}

fun openAppSettings(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    )
}

/** True if the user has turned on AuraDock under "Notification access" in system settings. */
fun isNotificationAccessEnabled(context: Context): Boolean {
    val enabledListeners = Settings.Secure.getString(
        context.contentResolver,
        "enabled_notification_listeners"
    ) ?: return false
    return enabledListeners.contains(context.packageName)
}

/** Opens the system's "Do Not Disturb access" list, where the user can switch AuraDock on. */
fun openDndAccessSettings(context: Context) {
    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

/** Android 12+ makes exact alarms opt-in; below that they are always allowed. */
fun canScheduleExactAlarms(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

/** Opens the system page where the user can allow AuraDock to set exact alarms. */
fun openExactAlarmSettings(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.startActivity(
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.fromParts("package", context.packageName, null))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

/**
 * Opens the system's screensaver settings. Some makers do not handle the direct page, so fall back to the display
 * settings, where the screensaver entry lives.
 */
fun openScreensaverSettings(context: Context) {
    try {
        context.startActivity(Intent(Settings.ACTION_DREAM_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: android.content.ActivityNotFoundException) {
        context.startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

/** Opens the system display settings, where the screen timeout lives. */
fun openDisplaySettings(context: Context) {
    context.startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

/**
 * True if Android lists AuraDock as the chosen screensaver. Reads the system's setting, which some makers keep
 * elsewhere; then this says false and the checkmark simply does not show.
 */
fun isAuraDockScreensaver(context: Context): Boolean {
    val chosen = Settings.Secure.getString(context.contentResolver, "screensaver_components") ?: return false
    return chosen.split(',').any { android.content.ComponentName.unflattenFromString(it.trim())?.packageName == context.packageName }
}
