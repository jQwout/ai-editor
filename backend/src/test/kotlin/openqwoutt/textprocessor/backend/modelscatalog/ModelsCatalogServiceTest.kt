package openqwoutt.textprocessor.backend.modelscatalog

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ModelsCatalogServiceTest {
    companion object {
        @BeforeAll
        @JvmStatic
        fun startPg() = ModelsCatalogTestSupport.initPostgres()

        @AfterAll
        @JvmStatic
        fun stopPg() = ModelsCatalogTestSupport.closePostgres()
    }

    private val service get() = ModelsCatalogTestSupport.service()

    @BeforeEach
    fun clean() = ModelsCatalogTestSupport.clearModels()

    @Test
    fun `sync rejects empty models list`() {
        assertThrows(IllegalArgumentException::class.java) {
            service.sync(models = emptyList(), disableMissing = true)
        }
    }

    @Test
    fun `sync rejects duplicate modelId`() {
        assertThrows(IllegalArgumentException::class.java) {
            service.sync(
                models =
                    listOf(
                        CatalogModelEntry(modelId = "dup", displayName = "A"),
                        CatalogModelEntry(modelId = "dup", displayName = "B"),
                    ),
                disableMissing = true,
            )
        }
    }

    @Test
    fun `sync trims model id and display name`() {
        service.sync(
            models =
                listOf(
                    CatalogModelEntry(
                        modelId = "  meta/llama  ",
                        displayName = "  Llama  ",
                    ),
                ),
            disableMissing = true,
        )

        val snap = service.catalog()
        assertEquals("meta/llama", snap.models.single().modelId)
        assertEquals("Llama", snap.models.single().displayName)
    }

    @Test
    fun `sync with disableMissing false leaves stale models enabled`() {
        service.sync(
            models =
                listOf(
                    CatalogModelEntry(modelId = "a", displayName = "A"),
                    CatalogModelEntry(modelId = "b", displayName = "B"),
                ),
            disableMissing = true,
        )
        service.sync(
            models = listOf(CatalogModelEntry(modelId = "a", displayName = "A")),
            disableMissing = false,
        )

        assertEquals(2, service.catalog().models.size)
    }

    @Test
    fun `sync with disableMissing true disables stale models`() {
        service.sync(
            models =
                listOf(
                    CatalogModelEntry(modelId = "a", displayName = "A"),
                    CatalogModelEntry(modelId = "b", displayName = "B"),
                ),
            disableMissing = true,
        )
        val result =
            service.sync(
                models = listOf(CatalogModelEntry(modelId = "a", displayName = "A")),
                disableMissing = true,
            )

        assertEquals(0, result.upserted)
        assertEquals(1, result.unchanged)
        assertEquals(1, result.disabled)
        assertEquals(1, service.catalog().models.size)
    }

    @Test
    fun `sync re-enables previously disabled model`() {
        service.sync(
            models = listOf(CatalogModelEntry(modelId = "m", displayName = "M", isEnabled = false)),
            disableMissing = true,
        )
        assertTrue(service.catalog().models.isEmpty())

        service.sync(
            models = listOf(CatalogModelEntry(modelId = "m", displayName = "M", isEnabled = true)),
            disableMissing = true,
        )
        assertEquals(1, service.catalog().models.size)
    }

    @Test
    fun `sync reports unchanged on identical resync`() {
        val batch = listOf(CatalogModelEntry(modelId = "x", displayName = "X"))
        val first = service.sync(models = batch, disableMissing = true)
        assertEquals(1, first.upserted)
        assertEquals(0, first.unchanged)

        val second = service.sync(models = batch, disableMissing = true)
        assertEquals(0, second.upserted)
        assertEquals(1, second.unchanged)
    }
}
