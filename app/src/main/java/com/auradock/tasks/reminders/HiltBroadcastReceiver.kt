package com.auradock.tasks.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.annotation.CallSuper

/**
 * BroadcastReceiver.onReceive is abstract, so Kotlin will not let a Hilt receiver call `super.onReceive`, but Hilt's
 * generated code needs that call to inject the @Inject fields. This base class gives Hilt receivers a real
 * (empty) onReceive to call.
 */
abstract class HiltBroadcastReceiver : BroadcastReceiver() {
    @CallSuper
    override fun onReceive(context: Context, intent: Intent) {
    }
}
