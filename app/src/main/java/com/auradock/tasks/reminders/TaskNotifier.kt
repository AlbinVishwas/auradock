package com.auradock.tasks.reminders

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.auradock.MainActivity
import com.auradock.R
import com.auradock.tasks.TaskEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/** Creates the "Task reminders" channel and posts the notification for a due task. */
@Singleton
class TaskNotifier @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Since Android 8 every notification belongs to a channel, and the user controls sound and importance per
     * channel in system settings. Creating an existing channel again does nothing, so this is safe to repeat.
     */
    fun ensureChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Task reminders", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Tells you when a task with a time is due"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    @SuppressLint("MissingPermission") // checked just below with areNotificationsEnabled()
    fun show(task: TaskEntity) {
        ensureChannel()
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return // the user said no (Android 13+ permission or settings)

        val open = PendingIntent.getActivity(
            context,
            task.id.toInt(),
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val dueText = task.dueAt?.let { "Due " + DateFormat.getTimeFormat(context).format(Date(it)) } ?: "Due now"
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_task)
            .setContentTitle(task.title)
            .setContentText(dueText)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setWhen(task.dueAt ?: System.currentTimeMillis())
            .setAutoCancel(true)
            .setContentIntent(open) // tapping it opens the dashboard
            .build()
        manager.notify(task.id.toInt(), notification)
    }

    companion object {
        const val CHANNEL_ID = "task_reminders"
    }
}
