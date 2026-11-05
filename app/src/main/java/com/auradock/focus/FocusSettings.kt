package com.auradock.focus

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** The user's Focus choices, saved on the phone. Shared by the app and the screensaver. */
@Singleton
class FocusSettings @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("auradock_focus_settings", Context.MODE_PRIVATE)

    private val _focusWhileDocked = MutableStateFlow(prefs.getBoolean(KEY_FOCUS, true))

    /** On by default: it only does anything once the user has also granted DND access. */
    val focusWhileDocked: StateFlow<Boolean> = _focusWhileDocked.asStateFlow()

    fun setFocusWhileDocked(enabled: Boolean) {
        _focusWhileDocked.value = enabled
        prefs.edit().putBoolean(KEY_FOCUS, enabled).apply()
    }

    private companion object {
        const val KEY_FOCUS = "focus_while_docked"
    }
}
