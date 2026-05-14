package openqwoutt.textprocessor.backend.security

import java.security.MessageDigest

/**
 * Constant-time comparison of two UTF-8 strings by comparing SHA-256 digests
 * (avoids early exit on length mismatch of raw bytes).
 */
fun secureEqualsUtf8Strings(a: String, b: String): Boolean {
    val digest = MessageDigest.getInstance("SHA-256")
    val ha = digest.digest(a.toByteArray(Charsets.UTF_8))
    val hb = MessageDigest.getInstance("SHA-256").digest(b.toByteArray(Charsets.UTF_8))
    return MessageDigest.isEqual(ha, hb)
}
