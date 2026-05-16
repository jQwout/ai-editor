package openqwoutt.textprocessor.backend.bootstrap

import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.ArrayDeque

/**
 * Fixed-window-ish limiter: per [key], at most [maxRequests] calls succeed within any sliding [windowMillis].
 * Coarse global lock — sufficient for modest QPS on a single backend instance.
 */
class SlidingWindowRateLimiter(
    private val maxRequests: Int,
    private val windowMillis: Long,
) {
    private val eventsByKey = ConcurrentHashMap<String, ArrayDeque<Long>>()
    private val gate = Any()

    fun tryAcquire(key: String): Boolean {
        val now = System.currentTimeMillis()
        synchronized(gate) {
            val q = eventsByKey.computeIfAbsent(key) { ArrayDeque() }
            while (q.isNotEmpty() && now - q.first() > windowMillis) {
                q.removeFirst()
            }
            if (q.size >= maxRequests) {
                return false
            }
            q.addLast(now)
            if (eventsByKey.size > 10_000) {
                eventsByKey.entries.removeIf { (_, dq) -> dq.isEmpty() }
            }
            return true
        }
    }
}
