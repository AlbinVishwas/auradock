package com.auradock.focus

import android.content.Context
import com.auradock.focus.vip.Vip
import com.auradock.focus.vip.decodeVips
import com.auradock.focus.vip.encodeVips
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

    private val _showMessageText = MutableStateFlow(prefs.getBoolean(KEY_TEXT, false))

    /** Off by default: the dock shows who a message is from, not what it says. */
    val showMessageText: StateFlow<Boolean> = _showMessageText.asStateFlow()

    fun setShowMessageText(show: Boolean) {
        _showMessageText.value = show
        prefs.edit().putBoolean(KEY_TEXT, show).apply()
    }

    private val _pickedApps = MutableStateFlow(
        prefs.getString(KEY_APPS, "").orEmpty().split(',').filter { it.isNotBlank() }.take(MAX_PICKED_APPS)
    )

    /** The (up to three) apps whose notifications the dock shows, in the order they were picked. */
    val pickedApps: StateFlow<List<String>> = _pickedApps.asStateFlow()

    /** Picks or un-picks [packageName]. Returns false if it could not be picked because three are already chosen. */
    fun toggleApp(packageName: String): Boolean {
        val next = togglePick(_pickedApps.value, packageName)
        if (next == _pickedApps.value) return false
        _pickedApps.value = next
        prefs.edit().putString(KEY_APPS, next.joinToString(",")).apply()
        return true
    }

    private val _dockFromAnywhere = MutableStateFlow(prefs.getBoolean(KEY_ANYWHERE, false))

    /** Off by default: when on, a background service watches for the phone being set on a stand (see DockWatcherService). */
    val dockFromAnywhere: StateFlow<Boolean> = _dockFromAnywhere.asStateFlow()

    fun setDockFromAnywhere(enabled: Boolean) {
        _dockFromAnywhere.value = enabled
        prefs.edit().putBoolean(KEY_ANYWHERE, enabled).apply()
    }

    private val _vips = MutableStateFlow(decodeVips(prefs.getString(KEY_VIPS, "").orEmpty()))

    /** The (up to five) people who can always reach the user. Names and contact keys only, kept on the phone. */
    val vips: StateFlow<List<Vip>> = _vips.asStateFlow()

    fun addVip(vip: Vip): Boolean {
        val next = com.auradock.focus.vip.addVip(_vips.value, vip)
        if (next == _vips.value) return false
        saveVips(next)
        return true
    }

    fun removeVip(lookupKey: String) = saveVips(com.auradock.focus.vip.removeVip(_vips.value, lookupKey))

    private fun saveVips(list: List<Vip>) {
        _vips.value = list
        prefs.edit().putString(KEY_VIPS, encodeVips(list)).apply()
    }

    private val _vipRingThrough = MutableStateFlow(prefs.getBoolean(KEY_VIP_RING, false))

    /** Off until the user switches it on (which also asks for the permission to change contacts). */
    val vipRingThrough: StateFlow<Boolean> = _vipRingThrough.asStateFlow()

    fun setVipRingThrough(enabled: Boolean) {
        _vipRingThrough.value = enabled
        prefs.edit().putBoolean(KEY_VIP_RING, enabled).apply()
    }

    private companion object {
        const val KEY_VIPS = "vips"
        const val KEY_VIP_RING = "vip_ring_through"
        const val KEY_ANYWHERE = "dock_from_anywhere"
        const val KEY_FOCUS = "focus_while_docked"
        const val KEY_APPS = "picked_apps"
        const val KEY_TEXT = "show_message_text"
    }
}
