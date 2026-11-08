package com.auradock.service

import android.content.ComponentName
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.auradock.data.NotificationRepository
import com.auradock.data.RawNotification
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Android tells this service about every notification; it passes them to [NotificationRepository], which filters them. */
@AndroidEntryPoint
class AuraNotificationListenerService : NotificationListenerService() {

    @Inject lateinit var repository: NotificationRepository

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "listener connected")
        // Load what is already in the shade, so the dock is not empty until the next notification arrives.
        repository.attach { (activeNotifications ?: emptyArray()).map(::toRaw) }
    }

    /**
     * The system disconnected us (Samsung's battery management can do this to background apps). Ask Android to bind us
     * again; if the user withdrew access this does nothing, which is correct.
     */
    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d(TAG, "listener disconnected, asking to be rebound")
        repository.detach()
        runCatching { requestRebind(ComponentName(this, AuraNotificationListenerService::class.java)) }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?, rankingMap: RankingMap?) {
        if (sbn != null) repository.onPosted(toRaw(sbn, rankingMap))
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        // Handled by the version above, which also gets the ranking.
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // Dismissed by the user, or withdrawn by the app: either way it must leave the dock.
        if (sbn != null) repository.onRemoved(sbn.key)
    }

    private fun toRaw(sbn: StatusBarNotification, rankingMap: RankingMap? = null): RawNotification {
        val extras = sbn.notification.extras
        val filter = runCatching { currentInterruptionFilter }.getOrDefault(INTERRUPTION_FILTER_UNKNOWN)
        val dndActive = filter != INTERRUPTION_FILTER_ALL && filter != INTERRUPTION_FILTER_UNKNOWN
        val allowed = runCatching {
            val ranking = Ranking()
            (rankingMap ?: currentRanking).getRanking(sbn.key, ranking) && ranking.matchesInterruptionFilter()
        }.getOrDefault(false)
        return RawNotification(
            key = sbn.key,
            packageName = sbn.packageName,
            appName = appLabel(sbn.packageName),
            title = extras.getCharSequence("android.title")?.toString().orEmpty(),
            text = extras.getCharSequence("android.text")?.toString().orEmpty(),
            postedAt = sbn.postTime,
            ongoing = sbn.isOngoing,
            groupSummary = (sbn.notification.flags and android.app.Notification.FLAG_GROUP_SUMMARY) != 0,
            dndActive = dndActive,
            allowedByDnd = allowed
        )
    }

    private fun appLabel(packageName: String): String = try {
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString()
    } catch (e: PackageManager.NameNotFoundException) {
        packageName.substringAfterLast('.')
    }

    private companion object {
        const val TAG = "AuraNotifListener"
    }
}
