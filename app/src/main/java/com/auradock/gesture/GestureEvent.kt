package com.auradock.gesture

/**
 * All gesture events recognised by [GestureEngine].
 * These are high-level semantic events, decoupled from raw [android.view.MotionEvent].
 */
sealed class GestureEvent {
    object Tap       : GestureEvent()
    object DoubleTap : GestureEvent()

    data class Swipe(val direction: Direction) : GestureEvent()

    enum class Direction { LEFT, RIGHT, UP, DOWN }

    override fun toString(): String = when (this) {
        is Tap       -> "Tap"
        is DoubleTap -> "DoubleTap"
        is Swipe     -> "Swipe(${direction.name})"
    }
}
