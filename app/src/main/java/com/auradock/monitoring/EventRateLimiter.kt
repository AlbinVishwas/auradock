package com.auradock.monitoring

/**
 * At most [limit] events of the same issue from this phone in any [windowMillis]: a loop that fails the same way again and again cannot flood
 * Sentry (or use up its quota) from one device. Counts live in memory, which is enough: a process restart is not a loop.
 */
class EventRateLimiter(private val limit: Int = 10, private val windowMillis: Long = 60 * 60 * 1000L) {
    private val seen = HashMap<String, ArrayDeque<Long>>()

    /** True when this event may be sent (and counts it), false when the issue has used up its share for this hour. */
    @Synchronized
    fun allow(key: String, nowMillis: Long): Boolean {
        val times = seen.getOrPut(key) { ArrayDeque() }
        while (times.isNotEmpty() && nowMillis - times.first() >= windowMillis) times.removeFirst()
        if (times.size >= limit) return false
        times.addLast(nowMillis)
        // Forget keys that have gone quiet, so the map cannot grow without end.
        if (seen.size > MAX_KEYS) seen.entries.removeAll { it.value.isEmpty() || nowMillis - it.value.last() >= windowMillis }
        return true
    }

    private companion object { const val MAX_KEYS = 200 }
}
