package com.auradock.monitoring

import android.app.Application
import android.content.Context
import com.auradock.BuildConfig
import io.sentry.Breadcrumb
import io.sentry.ISpan
import io.sentry.Sentry
import io.sentry.SentryEvent
import io.sentry.SentryLevel
import io.sentry.SentryOptions
import io.sentry.TransactionOptions
import io.sentry.android.core.SentryAndroid
import kotlin.random.Random

/** The one non-fatal report type: the message is a validated code, never text from the user (see [Scrubber]). */
class AuraNonFatal(code: String) : RuntimeException(code)

/**
 * Crash, ANR and performance reporting through Sentry, with no personal content (docs: CLAUDE.md, "Sentry").
 *
 *  - Off in debug builds, off when the DSN is missing, and off when the owner turns "Share crash reports" off (that closes Sentry at once).
 *  - Everything that leaves goes through [Scrubber]; identical events are limited to 10 an hour from this phone ([EventRateLimiter]).
 *  - Breadcrumbs and non-fatal reports carry codes, states and counts only. Use [breadcrumb] and [nonFatal]; never pass text from the user.
 */
object Monitoring {
    private const val PREFS = "auradock_monitoring"
    private const val KEY_SHARE = "share_crash_reports"

    private val limiter = EventRateLimiter()

    @Volatile private var enabledFlag = false
    private var dockStart: ISpan? = null
    private var dockSession: ISpan? = null
    private var pendingDockSource: String = "unknown"

    /** Whether the owner shares crash reports (default on). */
    fun isSharing(context: Context): Boolean = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_SHARE, true)

    /** Called once from the Application. */
    fun init(app: Application) {
        if (BuildConfig.DEBUG) return // debug builds never report (the test button on the debug page is the one exception, see [sendTestEvents])
        if (BuildConfig.SENTRY_DSN.isBlank() || !isSharing(app)) return
        start(app, environment = "production")
    }

    /** The "Share crash reports" switch: turning it off closes Sentry at once, turning it on starts it. */
    fun setSharing(app: Application, share: Boolean) {
        app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_SHARE, share).apply()
        if (share) {
            if (!BuildConfig.DEBUG && BuildConfig.SENTRY_DSN.isNotBlank() && !Sentry.isEnabled()) start(app, "production")
        } else {
            enabledFlag = false
            dockStart = null
            dockSession = null
            Sentry.close()
        }
    }

    private fun start(context: Context, environment: String) {
        SentryAndroid.init(context) { o: SentryOptions ->
            o.dsn = BuildConfig.SENTRY_DSN
            o.release = "com.auradock@${BuildConfig.VERSION_NAME}+${BuildConfig.VERSION_CODE}"
            o.environment = environment
            o.isDebug = environment == "dev" // the dev test run logs each envelope it sends (event ids only) so a send can be confirmed from logcat
            // Crashes and ANRs, and release health (sessions).
            o.isEnableUncaughtExceptionHandler = true
            (o as? io.sentry.android.core.SentryAndroidOptions)?.apply {
                isAnrEnabled = true
                isEnableAutoSessionTracking = true
                isEnableActivityLifecycleBreadcrumbs = true
                isEnableAppComponentBreadcrumbs = false
                isEnableSystemEventBreadcrumbs = false
                isEnableNetworkEventBreadcrumbs = false
                isEnableUserInteractionBreadcrumbs = false
                isEnableUserInteractionTracing = false
                isAttachScreenshot = false
                isAttachViewHierarchy = false
                isEnableFramesTracking = true // slow and frozen frames, attached to the dock's traces
                isEnableTimeToFullDisplayTracing = false
            }
            // Performance: 20% of traces in release builds (app start is automatic; screens and the dock are written by hand below).
            o.tracesSampleRate = if (environment == "dev") 1.0 else 0.2
            // No personal data of any kind.
            o.isSendDefaultPii = false
            o.setBeforeSend { event, _ -> filter(event) }
            o.setBeforeBreadcrumb { crumb, _ -> if (enabledFlag) Scrubber.scrubBreadcrumb(crumb) else null }
            o.setBeforeSendTransaction { tx, _ -> if (enabledFlag) Scrubber.scrubTransaction(tx) else null }
        }
        enabledFlag = true
    }

    /** The last gate before an event leaves: sharing on, nothing personal, and at most 10 of one issue an hour. */
    private fun filter(event: SentryEvent): SentryEvent? {
        if (!enabledFlag) return null
        val clean = Scrubber.scrubEvent(event)
        val key = issueKey(clean)
        return if (limiter.allow(key, System.currentTimeMillis())) clean else null
    }

    private fun issueKey(event: SentryEvent): String {
        event.fingerprints?.takeIf { it.isNotEmpty() }?.let { return it.joinToString("|") }
        val top = event.exceptions?.firstOrNull()
        val frame = top?.stacktrace?.frames?.lastOrNull { it.isInApp == true } ?: top?.stacktrace?.frames?.lastOrNull()
        return listOf(top?.type, frame?.module, frame?.function, frame?.lineno).joinToString(":")
    }

    // ── Breadcrumbs and non-fatal reports: codes, states and counts only ──────────────────────────────────────────────────

    /** A breadcrumb. [topic] is a short code (`screensaver.gate`), [state] a code for what happened, [data] numbers, booleans or codes. */
    fun breadcrumb(topic: String, state: String, vararg data: Pair<String, Any?>) {
        if (!enabledFlag) return
        val crumb = Breadcrumb().apply {
            category = Scrubber.OWN_PREFIX + topic
            message = state
            level = SentryLevel.INFO
            data.forEach { (k, v) -> Scrubber.safeValue(v)?.let { setData(k, it) } }
        }
        Sentry.addBreadcrumb(crumb)
    }

    /**
     * A non-fatal report. [code] is a short code (`dnd.restore_failed`). [sample] is the share of occurrences to send (weather and geocoder failures
     * use 0.1). Grouped by its code.
     */
    fun nonFatal(code: String, vararg data: Pair<String, Any?>, sample: Double = 1.0) {
        if (!enabledFlag || !Scrubber.isCode(code)) return
        if (sample < 1.0 && Random.nextDouble() >= sample) return
        Sentry.captureEvent(SentryEvent(AuraNonFatal(code)).apply {
            level = SentryLevel.WARNING
            fingerprints = listOf("aura.nonfatal", code)
            setTag(Scrubber.OWN_PREFIX + "code", code)
            data.forEach { (k, v) -> Scrubber.safeValue(v)?.let { setTag(Scrubber.OWN_PREFIX + k, it.toString()) } }
        })
    }

    // ── Traces ────────────────────────────────────────────────────────────────────────────────────────────────────────────

    /** A screen opening: a short transaction that ends once it has drawn. Returns a handle to finish. */
    fun startScreenLoad(screen: String): ISpan? {
        if (!enabledFlag || !Scrubber.isCode(screen)) return null
        return Sentry.startTransaction("screen.load.$screen", "ui.load", TransactionOptions().apply { isBindToScope = false })
    }

    fun finish(span: ISpan?) { span?.finish() }

    /** Where the dock is about to start from: `screensaver`, `dock_now` or `watcher`. Call before it shows. */
    fun markDockStart(source: String) {
        if (!enabledFlag || !Scrubber.isCode(source)) return
        pendingDockSource = source
        dockStart?.finish()
        dockStart = Sentry.startTransaction("dock.start.$source", "dock.start", TransactionOptions().apply { isBindToScope = false })
    }

    /** The dock has drawn its first frame: ends the start trace and starts the one that spans the time it is up (its slow and frozen frames). */
    fun dockShown() {
        if (!enabledFlag) return
        dockStart?.finish()
        dockStart = null
        dockSession?.finish()
        dockSession = Sentry.startTransaction("dock.visible.$pendingDockSource", "dock.visible", TransactionOptions().apply { isBindToScope = false })
    }

    fun dockHidden() {
        dockSession?.finish()
        dockSession = null
    }

    // ── The debug page's "Send test events" (debug builds only) ────────────────────────────────────────────────────────────

    /**
     * Debug builds only. Starts Sentry for this run in the `dev` environment, sends one crash-like event and one non-fatal report, and flushes.
     * Nothing here is sent from a release build.
     */
    fun sendTestEvents(app: Application) {
        if (!BuildConfig.DEBUG || BuildConfig.SENTRY_DSN.isBlank()) return
        if (!Sentry.isEnabled()) start(app, environment = "dev")
        breadcrumb("test", "send_test_events", "count" to 2)
        val crash = Sentry.captureException(IllegalStateException("test crash")) // the message is dropped by the scrubber; the stack trace stays
        nonFatal("test.non_fatal", "count" to 1)
        val nonFatalId = Sentry.getLastEventId()
        Sentry.flush(5000)
        android.util.Log.i("Monitoring", "test events sent: crash=$crash nonfatal=$nonFatalId")
    }
}
