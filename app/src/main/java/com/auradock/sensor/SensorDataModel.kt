package com.auradock.sensor

/**
 * Raw sensor reading snapshot — used for testing and debugging
 * without requiring real hardware.
 */
data class SensorDataModel(
    val x: Float,
    val y: Float,
    val z: Float,
    val timestampNs: Long = System.nanoTime()
)
