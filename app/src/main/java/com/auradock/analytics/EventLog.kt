package com.auradock.analytics

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** The local event log: a fire-and-forget [log] that writes one row, with a timestamp and no content. Never leaves the phone. */
@Singleton
class EventLog @Inject constructor(
    @ApplicationContext context: Context,
    private val dao: AppEventDao
) {
    private val prefs = context.getSharedPreferences("auradock_events", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun log(name: String, detail: String? = null, at: Long = System.currentTimeMillis()) {
        scope.launch { runCatching { dao.insert(AppEventEntity(name = name, at = at, detail = detail)) } }
    }

    /** Logs [name] only the first time ever (for "first dock", "screensaver set"). */
    fun logOnce(name: String, detail: String? = null) {
        if (prefs.getBoolean("once_$name", false)) return
        prefs.edit().putBoolean("once_$name", true).apply()
        log(name, detail)
    }

    suspend fun events(): List<AppEvent> = dao.all().map { AppEvent(it.name, it.at, it.detail) }
}
