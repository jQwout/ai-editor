package openqwoutt.textprocessor.backend.modelscatalog

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

class NvidiaModelDaoTest {
    companion object {
        @BeforeAll
        @JvmStatic
        fun startPg() = ModelsCatalogTestSupport.initPostgres()

        @AfterAll
        @JvmStatic
        fun stopPg() = ModelsCatalogTestSupport.closePostgres()
    }

    private val dao get() = ModelsCatalogTestSupport.dao()

    @BeforeEach
    fun clean() = ModelsCatalogTestSupport.clearModels()

    @Test
    fun `listEnabled returns only enabled models sorted by display name`() {
        dao.upsert("z-model", "Zulu", isEnabled = true)
        dao.upsert("a-model", "Alpha", isEnabled = true)
        dao.upsert("disabled", "Disabled", isEnabled = false)

        val rows = dao.listEnabled()
        assertEquals(2, rows.size)
        assertEquals("Alpha", rows[0].displayName)
        assertEquals("Zulu", rows[1].displayName)
    }

    @Test
    fun `upsert updates existing row`() {
        dao.upsert("meta/llama", "Old Name", isEnabled = true)
        dao.upsert("meta/llama", "New Name", isEnabled = true)

        val row = dao.listEnabled().single()
        assertEquals("New Name", row.displayName)
    }

    @Test
    fun `upsert can disable model`() {
        dao.upsert("meta/llama", "Llama", isEnabled = true)
        dao.upsert("meta/llama", "Llama", isEnabled = false)

        assertTrue(dao.listEnabled().isEmpty())
    }

    @Test
    fun `disableNotIn disables models outside set`() {
        dao.upsert("keep", "Keep", isEnabled = true)
        dao.upsert("drop", "Drop", isEnabled = true)

        val disabled = dao.disableNotIn(listOf("keep"))
        assertEquals(1, disabled)
        assertEquals(listOf("keep"), dao.listEnabled().map { it.modelId })
    }

    @Test
    fun `disableNotIn rejects empty modelIds`() {
        assertThrows(IllegalArgumentException::class.java) {
            dao.disableNotIn(emptyList())
        }
    }

    @Test
    fun `maxUpdatedAt returns null when table empty`() {
        assertNull(dao.maxUpdatedAt())
    }

    @Test
    fun `maxUpdatedAt returns latest timestamp after writes`() {
        dao.upsert("m1", "M1", isEnabled = true)
        val t1 = dao.maxUpdatedAt()
        assertNotNull(t1)

        Thread.sleep(5)
        dao.upsert("m2", "M2", isEnabled = true)
        val t2 = dao.maxUpdatedAt()
        assertNotNull(t2)
        assertTrue(t2!! >= t1!!)
    }

    @Test
    fun `catalogSnapshot uses epoch revision when table empty`() {
        val snap = dao.catalogSnapshot()
        assertEquals(Instant.EPOCH, snap.updatedAt)
        assertEquals(CatalogEtagRevision.EMPTY, snap.revision)
        assertTrue(snap.models.isEmpty())
    }

    @Test
    fun `catalogSnapshot includes enabled models and revision`() {
        dao.upsert("m1", "M1", isEnabled = true)
        val snap = dao.catalogSnapshot()
        assertEquals(1, snap.models.size)
        assertEquals(1, snap.revision.enabledCount)
        assertFalse(snap.updatedAt == Instant.EPOCH)
    }

    @Test
    fun `syncCatalog is atomic and returns applied counts`() {
        val first =
            dao.syncCatalog(
                models = listOf(CatalogModelEntry("a", "A"), CatalogModelEntry("b", "B")),
                disableMissing = true,
            )
        assertEquals(2, first.applied)
        assertEquals(0, first.unchanged)

        val noop =
            dao.syncCatalog(
                models = listOf(CatalogModelEntry("a", "A"), CatalogModelEntry("b", "B")),
                disableMissing = false,
            )
        assertEquals(0, noop.applied)
        assertEquals(2, noop.unchanged)
    }
}
