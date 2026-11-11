package com.auradock.analytics

import com.posthog.PostHog

/**
 * Remote switches (PostHog feature flags) for features that may need turning off. Every flag is ON when it cannot be read: no key, no network,
 * the user opted out, or the flags never loaded, so the app works the same offline. A flag set to false (or the string "false"/"off") in the
 * PostHog dashboard turns the feature off.
 */
object FeatureFlags {
    const val DOCK_FROM_ANYWHERE = "dock_from_anywhere"
    const val NOW_PLAYING = "now_playing"
    const val WORLD_TEMPLATE = "world_template"
    const val AI_FEATURES = "ai_features"

    val ALL = listOf(DOCK_FROM_ANYWHERE, NOW_PLAYING, WORLD_TEMPLATE, AI_FEATURES)

    /** Reading a flag value, replaceable in tests. */
    @Volatile internal var reader: (String) -> Any? = { key ->
        if (Analytics.isReady) runCatching { PostHog.getFeatureFlag(key) }.getOrNull() else null
    }

    fun isOn(name: String): Boolean = interpret(runCatching { reader(name) }.getOrNull())

    /** null (unknown) is ON; false, "false" and "off" are OFF; anything else is ON. */
    fun interpret(value: Any?): Boolean = when (value) {
        null -> true
        is Boolean -> value
        is String -> !(value.equals("false", true) || value.equals("off", true))
        else -> true
    }
}
