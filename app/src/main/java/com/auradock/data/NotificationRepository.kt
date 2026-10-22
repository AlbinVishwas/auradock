package com.auradock.data

import android.service.notification.StatusBarNotification
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NotificationItem(
    val packageName: String,
    val appName: String,
    val title: String,
    val content: String,
    val postedAt: Long
)

@Singleton
class NotificationRepository @Inject constructor() {

    val importantNotifications: StateFlow<List<NotificationItem>> = cache.asStateFlow()

    companion object {
        private const val TAG = "NotificationRepository"
        private const val MAX_ITEMS = 20

        private val importantPackages = setOf(
            "com.whatsapp",
            "com.google.android.gm",
            "com.google.android.apps.messaging",
            "com.slack",
            "com.microsoft.teams"
        )

        private val noisyPackages = setOf(
            "android",
            "com.android.systemui",
            "com.google.android.dialer"
        )

        private val cache = MutableStateFlow<List<NotificationItem>>(emptyList())

        fun onNotificationPosted(sbn: StatusBarNotification?) {
            if (sbn == null) return

            runCatching {
                val extras = sbn.notification.extras
                val title = extras.getCharSequence("android.title")?.toString().orEmpty()
                val content = extras.getCharSequence("android.text")?.toString().orEmpty()

                if (shouldIgnore(sbn.packageName, title, content)) return

                val item = NotificationItem(
                    packageName = sbn.packageName,
                    appName = sbn.packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() },
                    title = title.ifBlank { "Notification" },
                    content = content,
                    postedAt = sbn.postTime
                )

                val updated = (listOf(item) + cache.value)
                    .distinctBy { "${it.packageName}:${it.title}:${it.content}:${it.postedAt}" }
                    .take(MAX_ITEMS)

                cache.value = updated
                Log.d(TAG, "Notification captured from ${item.packageName}")
            }.onFailure {
                Log.e(TAG, "Failed processing notification", it)
            }
        }

        fun onNotificationRemoved(sbn: StatusBarNotification?) {
            if (sbn == null) return
            cache.value = cache.value.filterNot {
                it.packageName == sbn.packageName && it.postedAt == sbn.postTime
            }
        }

        private fun shouldIgnore(packageName: String, title: String, content: String): Boolean {
            if (packageName in noisyPackages) return true
            if (title.isBlank() && content.isBlank()) return true
            if (packageName !in importantPackages) return true
            return false
        }
    }
}
