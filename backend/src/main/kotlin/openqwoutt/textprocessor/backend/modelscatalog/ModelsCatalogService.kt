package openqwoutt.textprocessor.backend.modelscatalog

import io.micrometer.core.instrument.MeterRegistry
import openqwoutt.textprocessor.backend.observability.recordModelsCatalogSync
import java.time.Instant
import kotlin.system.measureTimeMillis

class ModelsCatalogService(
    private val dao: NvidiaModelDao,
    private val meterRegistry: MeterRegistry? = null,
) {
    fun catalog(): NvidiaCatalogSnapshot = dao.catalogSnapshot()

    fun sync(
        models: List<CatalogModelEntry>,
        disableMissing: Boolean,
    ): SyncResult {
        val normalized = validateCatalogModelBatch(models)
        return try {
            lateinit var sync: SyncDaoResult
            val durationMs =
                measureTimeMillis {
                    sync = dao.syncCatalog(models = normalized, disableMissing = disableMissing)
                }
            meterRegistry.recordModelsCatalogSync(
                durationMs = durationMs,
                success = true,
                applied = sync.applied,
                disabled = sync.disabled,
            )
            SyncResult(
                upserted = sync.applied,
                unchanged = sync.unchanged,
                disabled = sync.disabled,
                updatedAt = Instant.ofEpochMilli(sync.revision.updatedAtEpochMillis),
            )
        } catch (@Suppress("TooGenericExceptionCaught") t: Throwable) {
            meterRegistry.recordModelsCatalogSync(
                durationMs = 0L,
                success = false,
                applied = 0,
                disabled = 0,
            )
            throw t
        }
    }
}

data class SyncResult(
    /** Rows inserted or updated (excludes no-op conflicts). */
    val upserted: Int,
    val unchanged: Int,
    val disabled: Int,
    val updatedAt: Instant,
)
