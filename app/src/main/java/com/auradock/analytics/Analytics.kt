package com.auradock.analytics

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.auradock.BuildConfig
import com.posthog.PersonProfiles
import com.posthog.PostHog
import com.posthog.android.PostHogAndroid
import com.posthog.android.PostHogAndroidConfig

/**
 * Product analytics through PostHog: anonymous counts of how the app is used, never what the user wrote.
 *
 *  - Every event goes through [AnalyticsScrubber] (an allow-list of event names, property names and fixed values) before it is handed over.
 *  - No person profiles (`PersonProfiles.NEVER`), no session replay, no exception autocapture (Sentry owns crashes), no screen-view capture.
 *    App lifecycle events (installed, opened, backgrounded, updated) are on.
 *  - Off unless the project key was available at build time (`~/.auradock-signing/posthog.properties`), off in debug builds unless the debug
 *    flag is on (then `environment=dev`, and the separate dev project's key if there is one), and off while the Settings switch "Share usage
 *    analytics" is off. Turning it off calls `PostHog.optOut()` at once.
 */
object Analytics {
    private const val TAG = "Analytics"
    private const val PREFS = "auradock_analytics"
    private const val KEY_SHARE = "share"
    private const val KEY_DEBUG_ON = "debug_on"
    private const val KEY_FIRST_USE = "first_use_at"

    @Volatile private var ready = false

    /** Events handed to PostHog since this start, by name (debug and tests only: shows what is sent, never any values). */
    val sentNames: MutableList<String> = java.util.Collections.synchronizedList(mutableListOf())

    fun isSharing(context: Context): Boolean = prefs(context).getBoolean(KEY_SHARE, true)

    /** Debug builds only: whether analytics is switched on for this install (it goes to the dev project, with environment=dev). */
    fun isDebugOn(context: Context): Boolean = BuildConfig.DEBUG && prefs(context).getBoolean(KEY_DEBUG_ON, false)

    /** When the app was first used, for "after 7 days of use" (set on the first start; kept). */
    fun firstUseAt(context: Context): Long {
        val p = prefs(context)
        val saved = p.getLong(KEY_FIRST_USE, 0L)
        if (saved != 0L) return saved
        val now = System.currentTimeMillis()
        p.edit().putLong(KEY_FIRST_USE, now).apply()
        return now
    }

    /** Call once from [Application.onCreate]. Does nothing without a key, or in a debug build with the flag off. */
    fun init(app: Application) {
        firstUseAt(app)
        val key = when {
            BuildConfig.DEBUG -> if (isDebugOn(app)) BuildConfig.POSTHOG_DEV_KEY.ifBlank { BuildConfig.POSTHOG_KEY } else ""
            else -> BuildConfig.POSTHOG_KEY
        }
        if (key.isBlank()) {
            Log.i(TAG, "analytics off (no key for this build)")
            return
        }
        val config = PostHogAndroidConfig(apiKey = key, host = BuildConfig.POSTHOG_HOST).apply {
            personProfiles = PersonProfiles.NEVER
            captureScreenViews = false
            captureApplicationLifecycleEvents = true
            captureDeepLinks = false
            sessionReplay = false
            errorTrackingConfig.autoCapture = false
            sendFeatureFlagEvent = false
            preloadFeatureFlags = true
            flushAt = 20
            flushIntervalSeconds = 60
            surveys = true
            surveysConfig.surveysDelegate = SurveyBridge
            optOut = !isSharing(app)
        }
        PostHogAndroid.setup(app, config)
        if (BuildConfig.DEBUG) PostHog.register("\$environment", "dev")
        ready = true
        // Send what is waiting whenever the app goes to the background.
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) = flush()
        })
        Log.i(TAG, "analytics on (${if (BuildConfig.DEBUG) "dev" else "release"}), sharing=${isSharing(app)}")
    }

    /** The Settings switch. Off calls `PostHog.optOut()` immediately; on calls `optIn()`. */
    fun setSharing(context: Context, share: Boolean) {
        prefs(context).edit().putBoolean(KEY_SHARE, share).apply()
        if (!ready) return
        if (share) PostHog.optIn() else PostHog.optOut()
    }

    /** Debug builds: turn analytics on or off for this install. Takes effect at the next start. */
    fun setDebugOn(context: Context, on: Boolean) {
        if (BuildConfig.DEBUG) prefs(context).edit().putBoolean(KEY_DEBUG_ON, on).apply()
    }

    /** Sends one event: the name and properties go through [AnalyticsScrubber] first; anything not allowed is dropped. */
    fun capture(name: String, properties: Map<String, Any?> = emptyMap()) {
        val clean = AnalyticsScrubber.clean(name, properties) ?: return
        sentNames += name
        Log.i(TAG, "event $name ${clean.keys}") // the event name and the property names only, never a value
        if (!ready) return
        runCatching { PostHog.capture(event = name, properties = clean) }
    }

    /** Sends what is waiting now (the app went to the background, or a dock closed). */
    fun flush() {
        if (ready) runCatching { PostHog.flush() }
    }

    internal val isReady: Boolean get() = ready

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
