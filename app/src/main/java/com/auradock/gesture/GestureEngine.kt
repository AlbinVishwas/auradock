package com.auradock.gesture

import android.view.GestureDetector
import android.view.MotionEvent
import android.util.Log
import com.auradock.core.StandbyController
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * Translates raw [MotionEvent] input into high-level [GestureEvent] instances
 * and notifies [StandbyController] (e.g. to wake from STANDBY_SLEEP).
 *
 * Usage: obtain [gestureListener] and pass it to a [GestureDetector].
 * Forward [MotionEvent]s from the window via [onMotionEvent].
 */
@Singleton
class GestureEngine @Inject constructor(
    private val controller: StandbyController
) {
    private val SWIPE_VELOCITY_THRESHOLD = 800f   // px/s
    private val SWIPE_DISTANCE_THRESHOLD = 100f   // px

    fun onMotionEvent(event: MotionEvent, detector: GestureDetector): Boolean =
        detector.onTouchEvent(event)

    val gestureListener: GestureDetector.SimpleOnGestureListener =
        object : GestureDetector.SimpleOnGestureListener() {

            init {
                Log.d(TAG, "[GestureEngine] Gesture detection active — listening for tap, double-tap, and swipe")
            }

            override fun onSingleTapUp(e: MotionEvent): Boolean {
                Log.d(TAG, "[GestureEngine] TAP detected at (${e.x.toInt()}, ${e.y.toInt()})")
                dispatch(GestureEvent.Tap)
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                Log.d(TAG, "[GestureEngine] DOUBLE_TAP detected at (${e.x.toInt()}, ${e.y.toInt()})")
                dispatch(GestureEvent.DoubleTap)
                return true
            }

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                val dX = e2.x - (e1?.x ?: 0f)
                val dY = e2.y - (e1?.y ?: 0f)
                val isHorizontal = abs(dX) > abs(dY)

                if (isHorizontal && abs(dX) > SWIPE_DISTANCE_THRESHOLD &&
                    abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    val dir = if (velocityX > 0) GestureEvent.Direction.RIGHT
                              else GestureEvent.Direction.LEFT
                    Log.d(TAG, "[GestureEngine] SWIPE ${dir.name} detected (velocityX=%.0fpx/s, dX=%.0fpx)".format(velocityX, dX))
                    dispatch(GestureEvent.Swipe(dir))
                    return true
                }
                if (!isHorizontal && abs(dY) > SWIPE_DISTANCE_THRESHOLD &&
                    abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                    val dir = if (velocityY > 0) GestureEvent.Direction.DOWN
                              else GestureEvent.Direction.UP
                    Log.d(TAG, "[GestureEngine] SWIPE ${dir.name} detected (velocityY=%.0fpx/s, dY=%.0fpx)".format(velocityY, dY))
                    dispatch(GestureEvent.Swipe(dir))
                    return true
                }
                return false
            }
        }

    private fun dispatch(event: GestureEvent) {
        Log.d(TAG, "[GestureEngine] Dispatching gesture → $event to StandbyController")
        controller.onGestureReceived(event)
    }

    companion object {
        private const val TAG = "AuraDock"
    }
}
