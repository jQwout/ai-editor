package openqwoutt.textprocessor.backend.modelscatalog

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CatalogEtagTest {
    @Test
    fun `catalogEtag includes provider revision tuple`() {
        val revision =
            CatalogEtagRevision(
                updatedAtEpochMillis = 1_700_000_000_000L,
                enabledCount = 3,
                maxRowId = 42L,
            )
        assertEquals("\"nvidia-1700000000000-3-42\"", catalogEtag(revision))
    }

    @Test
    fun `catalogEtag changes when any revision field changes`() {
        val base = CatalogEtagRevision(100L, 1, 5L)
        assertEquals("\"nvidia-100-1-5\"", catalogEtag(base))
        assertEquals("\"nvidia-200-1-5\"", catalogEtag(base.copy(updatedAtEpochMillis = 200L)))
        assertEquals("\"nvidia-100-2-5\"", catalogEtag(base.copy(enabledCount = 2)))
        assertEquals("\"nvidia-100-1-9\"", catalogEtag(base.copy(maxRowId = 9L)))
    }

    @Test
    fun `ifNoneMatchSatisfies handles star weak prefix and lists`() {
        val etag = catalogEtag(CatalogEtagRevision(100L, 1, 1L))
        assertTrue(ifNoneMatchSatisfies("*", etag))
        assertTrue(ifNoneMatchSatisfies("W/\"nvidia-100-1-1\"", etag))
        assertTrue(ifNoneMatchSatisfies("\"nvidia-0-0-0\", \"nvidia-100-1-1\"", etag))
        assertFalse(ifNoneMatchSatisfies("\"nvidia-100-1-2\"", etag))
    }
}
