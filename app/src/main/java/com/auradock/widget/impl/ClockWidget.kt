package com.auradock.widget.impl

import android.util.Log
import com.auradock.widget.BaseWidget
import com.auradock.widget.WidgetData
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import java.util.TimeZone
import javax.inject.Inject

class ClockWidget @Inject constructor() : BaseWidget {

    override val widgetId     = "com.auradock.widget.clock"
    override val displayName  = "Clock"
    override val updateIntervalMs = 1000L  // self-updating every second

    override suspend fun onActivate() {
        Log.d(TAG, "[ClockWidget] Initialized — tick interval: ${updateIntervalMs}ms")
    }

    override fun dataFlow(): Flow<WidgetData> = flow {
        while (currentCoroutineContext().isActive) {
            val timeMillis = System.currentTimeMillis()
            val timezone   = TimeZone.getDefault().id
            Log.d(TAG, "[ClockWidget] Update — time=${timeMillis}ms, timezone=$timezone")
            emit(WidgetData.Clock(timeMillis = timeMillis, timezone = timezone))
            delay(updateIntervalMs)
        }
    }

    override suspend fun onSuspend()    { Log.d(TAG, "[ClockWidget] Suspended") }
    override suspend fun onDeactivate() { Log.d(TAG, "[ClockWidget] Deactivated") }

    companion object { private const val TAG = "AuraDock" }
}
