package com.auradock.data

import android.util.Log
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * The one-time move of the usage tables out of `auradock.db` (0.3.1 and older) into `auradock_local.db`.
 *
 * Safe to run on every start: it does nothing once the old tables are gone, copies with INSERT OR IGNORE (same ids, so a run that
 * was interrupted can be repeated), compares row counts, and only drops an old table after its copy is verified. If anything goes
 * wrong the old tables stay where they are and the next start tries again; nothing is lost.
 */
object DatabaseSplit {
    private const val TAG = "DatabaseSplit"

    /** Tables that moved, in the order they are copied. */
    val MOVED = listOf("focus_sessions", "session_checkpoint", "daily_intention", "app_event")

    /** Result of one run: how many tables were found in the old file, how many were verified and dropped. */
    data class Outcome(val found: Int, val moved: Int, val failed: Int)

    /**
     * [main] is the open `auradock.db`; [local] is the open `auradock_local.db` with its tables created. Row copy uses the main
     * connection with the local file attached, so each table is one statement.
     */
    fun run(main: SupportSQLiteDatabase, localPath: String): Outcome {
        val present = MOVED.filter { tableExists(main, it) }
        if (present.isEmpty()) return Outcome(0, 0, 0)
        var moved = 0
        var failed = 0
        try {
            main.execSQL("ATTACH DATABASE '${localPath.replace("'", "''")}' AS local")
            for (table in present) {
                try {
                    val cols = columns(main, table).joinToString(",") { "`$it`" }
                    main.execSQL("INSERT OR IGNORE INTO local.`$table` ($cols) SELECT $cols FROM main.`$table`")
                    val before = count(main, "main.`$table`")
                    val after = count(main, "local.`$table`")
                    if (after >= before) {
                        main.execSQL("DROP TABLE main.`$table`")
                        moved++
                    } else {
                        failed++
                        Log.w(TAG, "$table: copy incomplete ($after of $before), keeping the old table")
                    }
                } catch (e: Exception) {
                    failed++
                    Log.w(TAG, "$table: could not move, will retry next start", e)
                }
            }
        } catch (e: Exception) {
            failed = present.size - moved
            Log.w(TAG, "could not attach the local database, will retry next start", e)
        } finally {
            runCatching { main.execSQL("DETACH DATABASE local") }
        }
        Log.i(TAG, "moved $moved of ${present.size} tables to the local database ($failed failed)")
        return Outcome(present.size, moved, failed)
    }

    private fun tableExists(db: SupportSQLiteDatabase, name: String): Boolean =
        db.query("SELECT 1 FROM main.sqlite_master WHERE type = 'table' AND name = '$name'").use { it.moveToFirst() }

    private fun columns(db: SupportSQLiteDatabase, table: String): List<String> =
        db.query("PRAGMA local.table_info(`$table`)").use { c ->
            val names = mutableListOf<String>()
            while (c.moveToNext()) names += c.getString(c.getColumnIndexOrThrow("name"))
            names
        }

    private fun count(db: SupportSQLiteDatabase, table: String): Long =
        db.query("SELECT COUNT(*) FROM $table").use { if (it.moveToFirst()) it.getLong(0) else 0L }
}
