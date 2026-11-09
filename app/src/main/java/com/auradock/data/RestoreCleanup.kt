package com.auradock.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import java.io.File

/**
 * The database is backed up for the tasks in it, but it also holds focus sessions, the open-session checkpoint and the event
 * log, which must not travel to another phone. After a restore (a fresh install that already has a database file, with no
 * marker in the no-backup folder) those tables are emptied before anything reads them. An ordinary update keeps the marker
 * logic out of the way: the app was installed earlier than it was last updated.
 */
object RestoreCleanup {
    private const val MARKER = "restore_checked"

    fun run(context: Context) {
        val marker = File(context.noBackupFilesDir, MARKER)
        if (marker.exists()) return
        try {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            val freshInstall = info.firstInstallTime == info.lastUpdateTime
            val db = context.getDatabasePath("auradock.db")
            if (freshInstall && db.exists()) {
                SQLiteDatabase.openDatabase(db.path, null, SQLiteDatabase.OPEN_READWRITE).use { sql ->
                    for (table in listOf("focus_sessions", "session_checkpoint", "app_event")) {
                        runCatching { sql.execSQL("DELETE FROM $table") }
                    }
                }
                Log.i("AuraRestore", "restored data: session tables emptied")
            }
            marker.createNewFile()
        } catch (e: Exception) {
            Log.w("AuraRestore", "restore check failed", e)
        }
    }
}
