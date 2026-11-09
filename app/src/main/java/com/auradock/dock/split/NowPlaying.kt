package com.auradock.dock.split

import com.auradock.monitoring.Monitoring
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.core.graphics.drawable.toBitmap
import com.auradock.permissions.isNotificationAccessEnabled
import com.auradock.service.AuraNotificationListenerService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** What is playing, read from the active media session. [appIcon] is the playing app's own icon (never a fixed one). */
data class NowPlayingState(
    val title: String,
    val artist: String,
    val art: Bitmap?,
    val appIcon: Bitmap?,
    val appName: String,
    val playing: Boolean,
    val durationMs: Long,
    /** Where the track was at [positionAt] (an elapsedRealtime value) and how fast it moves, so progress can be worked out without asking again. */
    val positionMs: Long,
    val positionAt: Long,
    val speed: Float
) {
    /** How far through the track it is, 0..1 (0 when the length is unknown). */
    fun progress(nowElapsed: Long): Float {
        if (durationMs <= 0L) return 0f
        val pos = if (playing) positionMs + ((nowElapsed - positionAt) * speed).toLong() else positionMs
        return (pos.toFloat() / durationMs).coerceIn(0f, 1f)
    }
}

/**
 * Reads the active media session through the app's notification access (the same permission the dock strip uses): its title, artist, art, the
 * app that is playing, and the play state. With nothing playing the state is null, and the Now playing widget hides from its stack. Controls
 * go straight to the session.
 */
@Singleton
class NowPlayingRepository @Inject constructor(@ApplicationContext private val context: Context) {
    private val _state = MutableStateFlow<NowPlayingState?>(null)
    val state: StateFlow<NowPlayingState?> = _state.asStateFlow()

    private val main = Handler(Looper.getMainLooper())
    private var started = false
    private var current: MediaController? = null
    private val watched = mutableMapOf<MediaController, MediaController.Callback>()

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers -> onSessions(controllers.orEmpty()) }

    /** Starts listening (does nothing without notification access, or if already listening). Safe to call whenever the dock appears. */
    fun start() {
        if (started || !isNotificationAccessEnabled(context)) return
        val manager = context.getSystemService(MediaSessionManager::class.java) ?: return
        val component = ComponentName(context, AuraNotificationListenerService::class.java)
        runCatching {
            manager.addOnActiveSessionsChangedListener(sessionsListener, component, main)
            started = true
            Monitoring.breadcrumb("media_session", "connected")
            onSessions(manager.getActiveSessions(component))
        }
    }

    private fun onSessions(controllers: List<MediaController>) {
        // Watch every active session, so a change in any of them is noticed; show the one that is playing (or else a paused one).
        val stale = watched.keys - controllers.toSet()
        stale.forEach { c -> watched.remove(c)?.let { c.unregisterCallback(it) } }
        controllers.forEach { c ->
            if (c !in watched) {
                val cb = object : MediaController.Callback() {
                    override fun onPlaybackStateChanged(state: PlaybackState?) = refresh()
                    override fun onMetadataChanged(metadata: MediaMetadata?) = refresh()
                    override fun onSessionDestroyed() = refresh()
                }
                watched[c] = cb
                c.registerCallback(cb, main)
            }
        }
        refresh()
    }

    private fun refresh() {
        val controllers = watched.keys.toList()
        fun stateOf(c: MediaController) = c.playbackState?.state
        val chosen = controllers.firstOrNull { stateOf(it) == PlaybackState.STATE_PLAYING || stateOf(it) == PlaybackState.STATE_BUFFERING }
            ?: controllers.firstOrNull { stateOf(it) == PlaybackState.STATE_PAUSED }
        current = chosen
        _state.value = chosen?.let { build(it) }
    }

    private fun build(c: MediaController): NowPlayingState? {
        val meta = c.metadata ?: return null
        val title = meta.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE) ?: meta.getString(MediaMetadata.METADATA_KEY_TITLE) ?: return null
        val artist = meta.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: meta.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE) ?: ""
        val art = meta.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) ?: meta.getBitmap(MediaMetadata.METADATA_KEY_ART) ?: meta.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
        val ps = c.playbackState
        val pm = context.packageManager
        val (icon, name) = runCatching {
            val info = pm.getApplicationInfo(c.packageName, 0)
            pm.getApplicationIcon(info).toBitmap() to pm.getApplicationLabel(info).toString()
        }.getOrDefault(null to c.packageName)
        return NowPlayingState(
            title = title,
            artist = artist,
            art = art,
            appIcon = icon,
            appName = name,
            playing = ps?.state == PlaybackState.STATE_PLAYING || ps?.state == PlaybackState.STATE_BUFFERING,
            durationMs = meta.getLong(MediaMetadata.METADATA_KEY_DURATION),
            positionMs = ps?.position ?: 0L,
            positionAt = ps?.lastPositionUpdateTime ?: SystemClock.elapsedRealtime(),
            speed = ps?.playbackSpeed ?: 1f
        )
    }

    fun playPause() {
        val c = current ?: return
        val controls = c.transportControls
        if (_state.value?.playing == true) controls.pause() else controls.play()
    }

    fun next() { current?.transportControls?.skipToNext() }
    fun previous() { current?.transportControls?.skipToPrevious() }
}

/** The Now playing widget's controls. */
data class MediaControls(val playPause: () -> Unit = {}, val next: () -> Unit = {}, val previous: () -> Unit = {})
