package openqwoutt.textprocessor.backend.modelscatalog

import java.sql.Connection
import java.sql.ResultSet
import java.time.Instant
import javax.sql.DataSource

data class NvidiaModelRow(
    val id: Long,
    val modelId: String,
    val displayName: String,
    val isEnabled: Boolean,
)

data class NvidiaCatalogSnapshot(
    val updatedAt: Instant,
    val models: List<NvidiaModelRow>,
    val revision: CatalogEtagRevision,
)

data class SyncDaoResult(
    val applied: Int,
    val unchanged: Int,
    val disabled: Int,
    val revision: CatalogEtagRevision,
)

class NvidiaModelDao(private val ds: DataSource) {

    fun catalogSnapshot(): NvidiaCatalogSnapshot =
        ds.connection.use { conn ->
            readSnapshot(conn)
        }

    fun maxUpdatedAt(): Instant? =
        ds.connection.use { conn ->
            val revision = readRevision(conn)
            if (revision == CatalogEtagRevision.EMPTY) null else Instant.ofEpochMilli(revision.updatedAtEpochMillis)
        }

    /**
     * Atomic snapshot sync with advisory transaction lock (single writer at a time per DB).
     */
    fun syncCatalog(
        models: List<CatalogModelEntry>,
        disableMissing: Boolean,
    ): SyncDaoResult {
        require(models.isNotEmpty()) { "models must not be empty" }

        ds.connection.use { conn ->
            val prevAutoCommit = conn.autoCommit
            conn.autoCommit = false
            return try {
                acquireSyncLock(conn)

                var applied = 0
                for (m in models) {
                    applied += upsertOnConnection(conn, m.modelId, m.displayName, m.isEnabled)
                }
                val unchanged = models.size - applied

                val disabled =
                    if (disableMissing) {
                        disableNotInOnConnection(conn, models.map { it.modelId })
                    } else {
                        0
                    }

                val revision = readRevision(conn)
                conn.commit()
                SyncDaoResult(
                    applied = applied,
                    unchanged = unchanged,
                    disabled = disabled,
                    revision = revision,
                )
            } catch (t: Throwable) {
                runCatching { conn.rollback() }
                throw t
            } finally {
                conn.autoCommit = prevAutoCommit
            }
        }
    }

    fun listEnabled(): List<NvidiaModelRow> =
        ds.connection.use { conn -> listEnabled(conn) }

    fun upsert(modelId: String, displayName: String, isEnabled: Boolean) {
        ds.connection.use { conn -> upsertOnConnection(conn, modelId, displayName, isEnabled) }
    }

    fun disableNotIn(modelIds: Collection<String>): Int {
        require(modelIds.isNotEmpty()) { "modelIds must not be empty" }
        return ds.connection.use { conn -> disableNotInOnConnection(conn, modelIds) }
    }

    private fun acquireSyncLock(conn: Connection) {
        conn.prepareStatement("select pg_advisory_xact_lock(?)").use { ps ->
            ps.setLong(1, SYNC_ADVISORY_LOCK_KEY)
            ps.execute()
        }
    }

    /**
     * Reads revision and enabled models in a single SQL statement snapshot.
     */
    private fun readSnapshot(conn: Connection): NvidiaCatalogSnapshot =
        conn.prepareStatement(
            """
            with rev as (
              select
                coalesce((extract(epoch from max(updated_at)) * 1000)::bigint, 0) as max_updated_at_ms,
                coalesce(max(id), 0) as max_id,
                count(*) filter (where is_enabled)::int as enabled_count
              from nvidia_model
            ),
            enabled as (
              select id, model_id, display_name, is_enabled
              from nvidia_model
              where is_enabled = true
            )
            select
              rev.max_updated_at_ms as max_updated_at_ms,
              rev.max_id as max_id,
              rev.enabled_count as enabled_count,
              e.id as id,
              e.model_id as model_id,
              e.display_name as display_name,
              e.is_enabled as is_enabled
            from rev
            left join enabled e on true
            order by e.display_name asc nulls last, e.id asc nulls last
            """.trimIndent(),
        ).use { ps ->
            ps.executeQuery().use { rs ->
                var revision: CatalogEtagRevision? = null
                val models = buildList {
                    while (rs.next()) {
                        if (revision == null) {
                            revision =
                                CatalogEtagRevision(
                                    updatedAtEpochMillis = rs.getLong("max_updated_at_ms"),
                                    enabledCount = rs.getInt("enabled_count"),
                                    maxRowId = rs.getLong("max_id"),
                                )
                        }
                        if (rs.getObject("id") != null) {
                            add(mapRow(rs))
                        }
                    }
                }
                val snapshotRevision = revision ?: CatalogEtagRevision.EMPTY
                NvidiaCatalogSnapshot(
                    updatedAt = Instant.ofEpochMilli(snapshotRevision.updatedAtEpochMillis),
                    models = models,
                    revision = snapshotRevision,
                )
            }
        }

    private fun readRevision(conn: Connection): CatalogEtagRevision =
        conn.prepareStatement(
            """
            select
              coalesce((extract(epoch from max(updated_at)) * 1000)::bigint, 0) as max_updated_at_ms,
              coalesce(max(id), 0) as max_id,
              count(*) filter (where is_enabled)::int as enabled_count
            from nvidia_model
            """.trimIndent(),
        ).use { ps ->
            ps.executeQuery().use { rs ->
                rs.next()
                CatalogEtagRevision(
                    updatedAtEpochMillis = rs.getLong("max_updated_at_ms"),
                    enabledCount = rs.getInt("enabled_count"),
                    maxRowId = rs.getLong("max_id"),
                )
            }
        }

    private fun listEnabled(conn: Connection): List<NvidiaModelRow> =
        conn.prepareStatement(
            """
            select id, model_id, display_name, is_enabled
            from nvidia_model
            where is_enabled = true
            order by display_name asc
            """.trimIndent(),
        ).use { ps ->
            ps.executeQuery().use { rs ->
                buildList {
                    while (rs.next()) add(mapRow(rs))
                }
            }
        }

    /** @return 1 if row inserted or updated; 0 if conflict with no changes */
    private fun upsertOnConnection(
        conn: Connection,
        modelId: String,
        displayName: String,
        isEnabled: Boolean,
    ): Int =
        conn.prepareStatement(
            """
            insert into nvidia_model (model_id, display_name, is_enabled, updated_at)
            values (?, ?, ?, now())
            on conflict (model_id) do update set
              display_name = excluded.display_name,
              is_enabled = excluded.is_enabled,
              updated_at = now()
            where nvidia_model.display_name is distinct from excluded.display_name
               or nvidia_model.is_enabled is distinct from excluded.is_enabled
            """.trimIndent(),
        ).use { ps ->
            ps.setString(1, modelId)
            ps.setString(2, displayName)
            ps.setBoolean(3, isEnabled)
            ps.executeUpdate()
        }

    private fun disableNotInOnConnection(conn: Connection, modelIds: Collection<String>): Int {
        require(modelIds.isNotEmpty()) { "modelIds must not be empty" }
        val placeholders = modelIds.joinToString(",") { "?" }
        return conn.prepareStatement(
            """
            update nvidia_model
            set is_enabled = false, updated_at = now()
            where is_enabled = true and model_id not in ($placeholders)
            """.trimIndent(),
        ).use { ps ->
            var i = 1
            for (id in modelIds) {
                ps.setString(i++, id)
            }
            ps.executeUpdate()
        }
    }

    private fun mapRow(rs: ResultSet): NvidiaModelRow =
        NvidiaModelRow(
            id = rs.getLong("id"),
            modelId = rs.getString("model_id"),
            displayName = rs.getString("display_name"),
            isEnabled = rs.getBoolean("is_enabled"),
        )

    companion object {
        /** Stable key for `pg_advisory_xact_lock` (catalog sync serialization). */
        private const val SYNC_ADVISORY_LOCK_KEY: Long = 0x4E5644434154L // "NVDCAT"
    }
}
