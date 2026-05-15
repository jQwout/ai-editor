package openqwoutt.textprocessor.backend.modelscatalog

/**
 * Revision tuple used for strong ETags (avoids millisecond-only collisions).
 */
data class CatalogEtagRevision(
    val updatedAtEpochMillis: Long,
    val enabledCount: Int,
    val maxRowId: Long,
) {
    companion object {
        val EMPTY: CatalogEtagRevision =
            CatalogEtagRevision(
                updatedAtEpochMillis = 0L,
                enabledCount = 0,
                maxRowId = 0L,
            )
    }
}

fun catalogEtag(revision: CatalogEtagRevision): String =
  "\"$PROVIDER_NVIDIA-${revision.updatedAtEpochMillis}-${revision.enabledCount}-${revision.maxRowId}\""

fun normalizeEtagHeaderValue(value: String): String =
    value.trim().removePrefix("W/").trim().trim('"')

/** Returns true when [ifNoneMatchHeader] matches [etag] per common If-None-Match semantics. */
fun ifNoneMatchSatisfies(ifNoneMatchHeader: String?, etag: String): Boolean {
    if (ifNoneMatchHeader.isNullOrBlank()) return false
    val trimmed = ifNoneMatchHeader.trim()
    if (trimmed == "*") return true

    val target = normalizeEtagHeaderValue(etag)
    return trimmed.split(',')
        .map { normalizeEtagHeaderValue(it) }
        .any { it == target }
}
