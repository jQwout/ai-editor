package openqwoutt.textprocessor.backend.modelscatalog

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class CatalogModelValidationTest {
    @Test
    fun `rejects blank modelId`() {
        assertThrows(IllegalArgumentException::class.java) {
            validateCatalogModelBatch(listOf(CatalogModelEntry("  ", "Name")))
        }
    }

    @Test
    fun `rejects duplicate modelId`() {
        val err =
            assertThrows(IllegalArgumentException::class.java) {
                validateCatalogModelBatch(
                    listOf(
                        CatalogModelEntry("a", "A"),
                        CatalogModelEntry("a", "B"),
                    ),
                )
            }
        assertEquals(true, err.message?.contains("duplicate"))
    }

    @Test
    fun `trims and normalizes entries`() {
        val out =
            validateCatalogModelBatch(
                listOf(CatalogModelEntry("  meta/x  ", "  Llama  ")),
            )
        assertEquals("meta/x", out.single().modelId)
        assertEquals("Llama", out.single().displayName)
    }
}
