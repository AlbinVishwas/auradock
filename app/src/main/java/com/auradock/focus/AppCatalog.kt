package com.auradock.focus

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class AppEntry(val packageName: String, val label: String)

/** One entry per app, sorted by name regardless of capitals. */
fun sortedUniqueApps(entries: List<AppEntry>): List<AppEntry> =
    entries.distinctBy { it.packageName }.sortedBy { it.label.lowercase() }

/**
 * The apps the user can launch from their home screen. It asks Android for "apps with a launcher icon" (the `<queries>`
 * entry in the manifest allows exactly that); it does not use the broad QUERY_ALL_PACKAGES permission, which Google Play
 * restricts, so AuraDock never sees apps without a launcher icon.
 */
@Singleton
class AppCatalog @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun launchableApps(): List<AppEntry> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val entries = pm.queryIntentActivities(launcher, 0)
            .map { AppEntry(it.activityInfo.packageName, it.loadLabel(pm).toString()) }
            .filter { it.packageName != context.packageName }
        sortedUniqueApps(entries)
    }

    fun icon(packageName: String): Drawable? = runCatching { context.packageManager.getApplicationIcon(packageName) }.getOrNull()
}
