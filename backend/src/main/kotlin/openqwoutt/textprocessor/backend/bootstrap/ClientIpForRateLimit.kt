package openqwoutt.textprocessor.backend.bootstrap

/**
 * Resolves the client identity string for rate limiting.
 *
 * @param trustForwardedHeaders when false, [remoteHost] is always returned (no header spoofing).
 */
fun resolveRateLimitClientIp(
    remoteHost: String,
    trustForwardedHeaders: Boolean,
    xForwardedFor: String?,
    xRealIp: String?,
): String {
    if (!trustForwardedHeaders) return remoteHost
    if (!xForwardedFor.isNullOrBlank()) {
        val first = xForwardedFor.split(',').firstOrNull()?.trim()?.takeIf { it.isNotEmpty() }
        if (first != null) return first
    }
    val real = xRealIp?.trim()
    if (!real.isNullOrEmpty()) return real
    return remoteHost
}
