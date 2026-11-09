package com.auradock.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.SystemClock

/**
 * Debug builds only. `adb shell am broadcast -a com.auradock.TEST_MEDIA --es cmd start|stop|pause|play` creates, ends or flips a fake media
 * session (a title, an artist, a drawn cover and a 3-minute track), so Now playing can be tried without a music app.
 */
class TestMediaReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.getStringExtra("cmd")) {
            "start" -> start(context.applicationContext)
            "stop" -> { session?.release(); session = null }
            "pause" -> set(PlaybackState.STATE_PAUSED)
            "play" -> set(PlaybackState.STATE_PLAYING)
        }
    }

    private fun cover(): Bitmap = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888).also { b ->
        val c = Canvas(b)
        c.drawColor(Color.rgb(36, 74, 120))
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(250, 200, 90) }
        c.drawCircle(150f, 150f, 90f, p)
        p.color = Color.rgb(36, 74, 120)
        c.drawCircle(150f, 150f, 28f, p)
    }

    private fun start(context: Context) {
        session?.release()
        val s = MediaSession(context, "AuraDockTest")
        s.setCallback(object : MediaSession.Callback() {
            override fun onPlay() = set(PlaybackState.STATE_PLAYING)
            override fun onPause() = set(PlaybackState.STATE_PAUSED)
            override fun onSkipToNext() { title = "Next Song"; publish() }
            override fun onSkipToPrevious() { title = "Slow Morning"; publish() }
        })
        s.isActive = true
        session = s
        title = "Slow Morning"
        state = PlaybackState.STATE_PLAYING
        art = cover()
        publish()
    }

    private fun set(newState: Int) { state = newState; publish() }

    private fun publish() {
        val s = session ?: return
        s.setMetadata(
            MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, title)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, "Kavya Rao")
                .putLong(MediaMetadata.METADATA_KEY_DURATION, 180_000L)
                .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, art)
                .build()
        )
        s.setPlaybackState(
            PlaybackState.Builder()
                .setActions(PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or PlaybackState.ACTION_SKIP_TO_NEXT or PlaybackState.ACTION_SKIP_TO_PREVIOUS)
                .setState(state, 68_000L, if (state == PlaybackState.STATE_PLAYING) 1f else 0f, SystemClock.elapsedRealtime())
                .build()
        )
    }

    private companion object {
        var session: MediaSession? = null
        var title = "Slow Morning"
        var state = PlaybackState.STATE_PLAYING
        var art: Bitmap? = null
    }
}
