package com.auradock.watch

import com.auradock.ui.theme.AuraDockTypography
import com.auradock.ui.theme.AuraDockSpacing
import com.auradock.ui.theme.AuraDockSizes
import com.auradock.ui.theme.AuraDockRadii
import com.auradock.ui.theme.AuraDockMotion
import com.auradock.ui.theme.AuraDockColors
import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.auradock.dock.Pill

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
            y = (context.resources.displayMetrics.density * AuraDockSizes.pillBottomOffsetDp).toInt()
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
internal fun PillContent(pill: Pill?, visible: Boolean, onTap: () -> Unit, onCancel: () -> Unit) {
    // Keep drawing the last pill while it slides away, after the state has already gone back to "none".
    var lastPill by remember { mutableStateOf<Pill?>(null) }
    if (pill != null) lastPill = pill
    val shown = lastPill ?: return

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(AuraDockMotion.softSpring()) { it } + fadeIn(tween(AuraDockMotion.Fast)),
        exit = slideOutVertically(tween(AuraDockMotion.Standard)) { it } + fadeOut(tween(AuraDockMotion.Standard))
    ) {
        Surface(
            color = AuraDockColors.PillBackground,
            contentColor = AuraDockColors.PillText,
            shape = RoundedCornerShape(AuraDockRadii.pill),
            border = BorderStroke(AuraDockSizes.line, AuraDockColors.PillBorder),
            modifier = Modifier.padding(AuraDockSpacing.sm).clickable(onClick = onTap) // the whole pill docks
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = AuraDockSpacing.lg, end = AuraDockSpacing.sm, top = AuraDockSpacing.sm, bottom = AuraDockSpacing.sm)
            ) {
                when (shown) {
                    is Pill.Countdown -> {
                        Text("Docking in ${shown.secondsLeft}…", color = AuraDockColors.PillText, style = AuraDockTypography.PillText)
                        Spacer(Modifier.width(AuraDockSpacing.md))
                        PillAction("Cancel", Modifier.clickable(onClick = onCancel)) // a separate target: Cancel does not dock
                    }
                    Pill.Offer -> {
                        Text("Switch to dock?", color = AuraDockColors.PillText, style = AuraDockTypography.PillText)
                        Spacer(Modifier.width(AuraDockSpacing.md))
                        PillAction("Dock", Modifier)
                    }
                }
            }
        }
    }
}

/** The action on the pill: the cream accent with black text. */
@Composable
private fun PillAction(text: String, modifier: Modifier) {
    Text(
        text = text,
        color = AuraDockColors.OnAccent,
        style = AuraDockTypography.PillAction,
        modifier = modifier
            .clip(RoundedCornerShape(AuraDockRadii.pill))
            .background(AuraDockColors.Accent)
            .padding(horizontal = AuraDockSpacing.md, vertical = AuraDockSpacing.smd)
    )
}
