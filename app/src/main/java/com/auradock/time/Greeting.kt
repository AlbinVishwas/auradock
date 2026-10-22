package com.auradock.time

import java.time.Instant
import java.time.ZoneId

/**
 * "Good morning" / "Good afternoon" / "Good evening" / "Good night" for the given moment, read in the
 * device's time zone. Driven by the same minute-level time as the clock, so it flips by itself.
 *
 *  05:00-11:59 morning · 12:00-16:59 afternoon · 17:00-21:59 evening · 22:00-04:59 night
 */
fun greetingFor(millis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
    when (Instant.ofEpochMilli(millis).atZone(zone).hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Good night"
    }
