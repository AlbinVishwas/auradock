package com.auradock.onboarding

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Whether the first-launch walkthrough has been finished (or left), saved on the phone so it only ever shows once. */
@Singleton
class OnboardingState @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("auradock_prefs", Context.MODE_PRIVATE)

    private val _done = MutableStateFlow(prefs.getBoolean(KEY_DONE, false))
    val done: StateFlow<Boolean> = _done.asStateFlow()

    fun complete() {
        _done.value = true
        prefs.edit().putBoolean(KEY_DONE, true).apply()
    }

    private companion object {
        const val KEY_DONE = "onboarding_done"
    }
}
