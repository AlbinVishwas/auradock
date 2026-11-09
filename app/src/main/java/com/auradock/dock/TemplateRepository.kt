package com.auradock.dock

import android.content.Context
import com.auradock.ui.theme.BoldPalette
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The user's chosen dock template, saved to the phone so it survives restarts.
 *
 * It is stored in SharedPreferences, Android's tiny key/value file for app settings: one entry,
 * "dock_template" = the enum name (for example "MINIMAL"). On creation we read it back into [selected]; every
 * [select] updates [selected] (so every screen reacts at once) and writes it to the file. As a singleton, the
 * app and the screensaver share the same instance and always agree.
 */
@Singleton
class TemplateRepository @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _selected = MutableStateFlow(parseDockTemplate(prefs.getString(KEY_TEMPLATE, null)))
    val selected: StateFlow<DockTemplate> = _selected.asStateFlow()

    private val _boldPalette = MutableStateFlow(BoldPalette.parse(prefs.getString(KEY_BOLD_PALETTE, null)))

    /** The Bold clock's colourway, saved on the phone. */
    val boldPalette: StateFlow<BoldPalette> = _boldPalette.asStateFlow()

    fun selectBoldPalette(palette: BoldPalette) {
        _boldPalette.value = palette
        prefs.edit().putString(KEY_BOLD_PALETTE, palette.name).apply()
    }

    fun select(template: DockTemplate) {
        _selected.value = template
        prefs.edit().putString(KEY_TEMPLATE, template.name).apply()
    }

    private companion object {
        const val PREFS_NAME = "auradock_prefs"
        const val KEY_TEMPLATE = "dock_template"
        const val KEY_BOLD_PALETTE = "bold_palette"
    }
}
