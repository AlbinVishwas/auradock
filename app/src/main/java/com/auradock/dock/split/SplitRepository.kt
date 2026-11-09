package com.auradock.dock.split

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** The Split dock's two stacks, saved on the phone and shared by the app and the screensaver (one instance). */
@Singleton
class SplitRepository @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences("auradock_split", Context.MODE_PRIVATE)
    private val _stacks = MutableStateFlow(decodeStacks(prefs.getString(KEY, null)))
    val stacks: StateFlow<SplitStacks> = _stacks.asStateFlow()

    fun setStack(side: SplitSide, stack: List<WidgetKind>) {
        val next = _stacks.value.with(side, stack)
        _stacks.value = next
        prefs.edit().putString(KEY, encodeStacks(next)).apply()
    }

    private companion object { const val KEY = "stacks" }
}
