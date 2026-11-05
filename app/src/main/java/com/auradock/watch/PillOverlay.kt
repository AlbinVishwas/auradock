package com.auradock.watch

import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.auradock.dock.Pill

private val Coral = Color(0xFFFF6B5E)

/**
 * The "Dock from anywhere" pill, drawn in a small window over whatever app is on screen (needs "Display over other apps").
 * The window only covers the pill, so touches everywhere else reach the app underneath.
 *
 * [owner] gives Compose the lifecycle and saved-state it looks for in a view tree; a Service has neither of its own.
 */
class PillOverlay(
    private val context: Context,
    private val owner: SavedStateRegistryOwner,
    private val onTap: () -> Unit,
    private val onCancel: () -> Unit
) {
    private val windowManager = context.getSystemService(WindowManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private var view: ComposeView? = null

    // Compose reads these, so changing them redraws the pill.
    private var pill by mutableStateOf<Pill?>(null)
    private var visible by mutableStateOf(false)

    private val removeView = Runnable { detach() }

    fun show(next: Pill) {
        handler.removeCallbacks(removeView)
        if (view == null && !attach()) return
        pill = next
        visible = true
    }

    /** Slides the pill away, then removes its window. */
    fun hide() {
        if (view == null) return
        visible = false
        handler.removeCallbacks(removeView)
        handler.postDelayed(removeView, EXIT_MS)
    }

    fun release() {
        handler.removeCallbacks(removeView)
        detach()
    }

    private fun attach(): Boolean {
        val composeView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setContent { PillContent(pill, visible, onTap, onCancel) }
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = (context.resources.displayMetrics.density * 72).toInt()
        }
        return runCatching { windowManager.addView(composeView, params) }
            .onSuccess { view = composeView }
            .onFailure { Log.w(TAG, "Could not show the pill (is \"Display over other apps\" still allowed?)", it) }
            .isSuccess
    }

    private fun detach() {
        val current = view ?: return
        view = null
        runCatching { windowManager.removeView(current) }
    }

    private companion object {
        const val TAG = "DockWatcher"
        const val EXIT_MS = 400L
    }
}

@Composable
private fun PillContent(pill: Pill?, visible: Boolean, onTap: () -> Unit, onCancel: () -> Unit) {
    // Keep drawing the last pill while it slides away, after the state has already gone back to "none".
    var lastPill by remember { mutableStateOf<Pill?>(null) }
    if (pill != null) lastPill = pill
    val shown = lastPill ?: return

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMediumLow)) { it } + fadeIn(tween(150)),
        exit = slideOutVertically(tween(250)) { it } + fadeOut(tween(250))
    ) {
        Surface(
            color = Color.Black,
            contentColor = Color.White,
            shape = RoundedCornerShape(percent = 50),
            border = BorderStroke(1.dp, Color(0x33FFFFFF)),
            shadowElevation = 8.dp,
            modifier = Modifier.padding(8.dp).clickable(onClick = onTap) // the whole pill docks
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 24.dp, end = 8.dp, top = 6.dp, bottom = 6.dp)
            ) {
                when (shown) {
                    is Pill.Countdown -> {
                        Text("Docking in ${shown.secondsLeft}…", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.width(16.dp))
                        Text(
                            text = "Cancel",
                            color = Coral,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clickable(onClick = onCancel) // a separate target: Cancel does not dock
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }
                    Pill.Offer -> {
                        Text("Switch to dock?", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.width(16.dp))
                        Text(
                            text = "Dock",
                            color = Coral,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }
    }
}
