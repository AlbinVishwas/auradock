package com.auradock

import android.app.Application
import com.auradock.focus.DockFocusController
import com.auradock.sessions.SessionTracker
import com.auradock.tasks.reminders.TaskNotifier
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/** Application entry point. @HiltAndroidApp triggers Hilt's code generation. */
@HiltAndroidApp
class AuraDockApplication : Application() {
    @Inject lateinit var taskNotifier: TaskNotifier
    @Inject lateinit var dockFocus: DockFocusController
    @Inject lateinit var sessionTracker: SessionTracker

    override fun onCreate() {
        super.onCreate() // Hilt injects taskNotifier here
        taskNotifier.ensureChannel()
        // If the app was killed while docked, Do Not Disturb may still be ours: no dock is showing in a fresh process, so
        // this puts the user's own settings back.
        dockFocus.refresh()
        // A session left open by a kill is closed at its last checkpoint.
        sessionTracker.recoverOpenSession()
    }
}
