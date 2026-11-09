package com.auradock.screensaver

import android.content.Context
import android.provider.Settings
import com.auradock.permissions.isAuraDockScreensaver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** The three steps of the guided test, as the screens draw them. */
data class ScreensaverTestUiState(
    /** Step 1: AuraDock is chosen as the screensaver. */
    val selected: Boolean = false,
    /** Step 2: the screen times out soon enough. */
    val timeoutOk: Boolean = false,
    /** Step 3: the dream has started by itself ("AuraDock appeared on its own"). */
    val appeared: Boolean = false
)

/**
 * The thin Android layer for the live screensaver test. The dream reports when it really starts (the orientation gate passed:
 * [onDreamShown]); the next time the app opens ([onAppOpened]) that is noted as done and the dashboard says so once. Stored on
 * the phone only. All the decisions are in [screensaverBanner].
 */
@Singleton
class ScreensaverTest @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("auradock_prefs", Context.MODE_PRIVATE)

    private val _steps = MutableStateFlow(ScreensaverTestUiState())
    val steps: StateFlow<ScreensaverTestUiState> = _steps.asStateFlow()

    private val _banner = MutableStateFlow(ScreensaverBanner.NONE)
    val banner: StateFlow<ScreensaverBanner> = _banner.asStateFlow()

    /** The dream is on screen and the phone is on a stand: the first time is what the test waits for. */
    fun onDreamShown() {
        if (prefs.getLong(KEY_DREAM_AT, 0L) == 0L) prefs.edit().putLong(KEY_DREAM_AT, System.currentTimeMillis()).apply()
    }

    /** The app opened: if the dream has appeared since, note it as verified (the dashboard shows the note until dismissed). */
    fun onAppOpened() {
        if (prefs.getLong(KEY_DREAM_AT, 0L) > 0 && prefs.getLong(KEY_VERIFIED_AT, 0L) == 0L) {
            prefs.edit().putLong(KEY_VERIFIED_AT, System.currentTimeMillis()).apply()
        }
        refresh()
    }

    fun dismissBanner() {
        prefs.edit().putBoolean(KEY_BANNER_DISMISSED, true).apply()
        refresh()
    }

    /** "Try again" on the hint: it is asked again in a day. */
    fun snoozeHint() {
        prefs.edit().putLong(KEY_HINT_SNOOZED_UNTIL, System.currentTimeMillis() + SCREENSAVER_HINT_AFTER_MS).apply()
        refresh()
    }

    /** Re-reads the phone's settings and our own record. Call when the app comes back to the front. */
    fun refresh() {
        val timeout = runCatching { Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, 0).toLong() }.getOrDefault(0L)
        _steps.value = ScreensaverTestUiState(
            selected = isAuraDockScreensaver(context),
            timeoutOk = timeoutOk(timeout),
            appeared = prefs.getLong(KEY_VERIFIED_AT, 0L) > 0 || prefs.getLong(KEY_DREAM_AT, 0L) > 0
        )
        _banner.value = screensaverBanner(
            dreamAt = prefs.getLong(KEY_DREAM_AT, 0L),
            verifiedAt = prefs.getLong(KEY_VERIFIED_AT, 0L),
            bannerDismissed = prefs.getBoolean(KEY_BANNER_DISMISSED, false),
            installedAt = installedAt(),
            now = System.currentTimeMillis(),
            hintSnoozedUntil = prefs.getLong(KEY_HINT_SNOOZED_UNTIL, 0L)
        )
    }

    private fun installedAt(): Long = runCatching { context.packageManager.getPackageInfo(context.packageName, 0).firstInstallTime }
        .getOrDefault(System.currentTimeMillis())

    private companion object {
        const val KEY_DREAM_AT = "screensaver_dream_at"
        const val KEY_VERIFIED_AT = "screensaver_verified_at"
        const val KEY_BANNER_DISMISSED = "screensaver_banner_dismissed"
        const val KEY_HINT_SNOOZED_UNTIL = "screensaver_hint_snoozed_until"
    }
}
