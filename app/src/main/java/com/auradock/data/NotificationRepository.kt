package com.auradock.data

import android.util.Log
import com.auradock.focus.FocusSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The notifications from the user's picked apps, kept in memory only (never saved to disk) and fed by the notification
 * listener service. A normal injected singleton: the service, the dock and the screensaver all share this one instance.
 */
@Singleton
class NotificationRepository @Inject constructor(
    private val settings: FocusSettings
) {
    private val _items = MutableStateFlow<List<NotificationItem>>(emptyList())
    val items: StateFlow<List<NotificationItem>> = _items.asStateFlow()

    /** Reads the notifications currently showing in the shade; set while the listener is connected. */
    @Volatile private var activeSource: (() -> List<RawNotification>)? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        // When the user changes their picked apps, rebuild from what is showing right now (a newly picked app's existing
        // notifications appear; a dropped app's disappear).
        scope.launch { settings.pickedApps.drop(1).collect { rebuild() } }
    }

    fun attach(source: () -> List<RawNotification>) {
        activeSource = source
        rebuild()
    }

    fun detach() {
        activeSource = null
    }

    fun onPosted(raw: RawNotification) {
        val picked = settings.pickedApps.value.toSet()
        if (isRelevant(raw, picked)) {
            _items.update { upsert(it, raw.toItem()) }
            Log.d(TAG, "cached ${raw.packageName} (${_items.value.size} total)")
        } else {
            // An update that no longer qualifies (it became "ongoing", say) must not leave the old version behind.
            _items.update { removeByKey(it, raw.key) }
        }
    }

    fun onRemoved(key: String) {
        _items.update { removeByKey(it, key) }
        Log.d(TAG, "removed (${_items.value.size} left)")
    }

    private fun rebuild() {
        val picked = settings.pickedApps.value.toSet()
        val source = activeSource
        _items.value = if (source == null) {
            retainPicked(_items.value, picked)
        } else {
            runCatching { source() }.getOrDefault(emptyList())
                .filter { isRelevant(it, picked) }
                .map { it.toItem() }
                .sortedByDescending { it.postedAt }
                .take(MAX_CACHED_NOTIFICATIONS)
        }
        Log.d(TAG, "rebuilt: ${_items.value.size} from picked apps $picked")
    }

    private companion object {
        const val TAG = "NotificationRepo"
    }
}
