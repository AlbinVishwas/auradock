package com.auradock.core

/**
 * Sealed class representing all possible standby states.
 * [StandbyController] is the single source of truth that holds this state.
 */
sealed class StandbyState {
    /** Phone is idle — not charging and not docked. */
    object IDLE : StandbyState()

    /** Phone is charging but not yet in landscape/dock position. */
    object CHARGING : StandbyState()

    /** Charging + landscape confirmed — full standby, widgets active. */
    object STANDBY_ACTIVE : StandbyState()

    /**
     * No user interaction for [sleepTimeoutMs] milliseconds.
     * Heavy widget operations are suspended; clock-level ops continue.
     */
    data class STANDBY_SLEEP(val sleepTimeoutMs: Long = 60_000L) : StandbyState()

    override fun toString(): String = this::class.simpleName ?: super.toString()
}
