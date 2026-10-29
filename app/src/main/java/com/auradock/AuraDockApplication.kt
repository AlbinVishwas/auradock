package com.auradock

import android.app.Application
import com.auradock.tasks.reminders.TaskNotifier
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/** Application entry point. @HiltAndroidApp triggers Hilt's code generation. */
@HiltAndroidApp
class AuraDockApplication : Application() {
    @Inject lateinit var taskNotifier: TaskNotifier

    override fun onCreate() {
        super.onCreate() // Hilt injects taskNotifier here
        taskNotifier.ensureChannel()
    }
}
