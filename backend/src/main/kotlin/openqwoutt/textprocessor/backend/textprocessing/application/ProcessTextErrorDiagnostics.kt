package openqwoutt.textprocessor.backend.textprocessing.application

internal object ProcessTextErrorDiagnostics {
    const val MAX_PROVIDER_RAW_CHARS: Int = 100_000

    fun truncateProviderRaw(raw: String?): String? {
        if (raw == null) return null
        if (raw.length <= MAX_PROVIDER_RAW_CHARS) return raw
        val n = raw.length - MAX_PROVIDER_RAW_CHARS
        return raw.take(MAX_PROVIDER_RAW_CHARS) + "\n...[truncated $n chars]"
    }

    /** Same as [truncateProviderRaw] but reports whether the string was shortened. */
    fun truncateProviderRawWithFlag(raw: String?): Pair<String?, Boolean> {
        if (raw == null) return null to false
        if (raw.length <= MAX_PROVIDER_RAW_CHARS) return raw to false
        return checkNotNull(truncateProviderRaw(raw)) to true
    }
}
