package openqwoutt.textprocessor.backend.promptstore

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import java.util.UUID
import javax.sql.DataSource

class PromptStoreDao(
    private val ds: DataSource,
    private val json: Json,
) {
    data class UpsertResult(val id: String, val upserted: Boolean)

    fun upsertPrompt(req: AdminUpsertPromptRequest): UpsertResult {
        val modeId = req.modeId.trim()
        val modelId = req.modelId?.trim()?.takeIf { it.isNotBlank() }
        val newId = UUID.randomUUID().toString()

        val tagsEl: JsonArray = buildJsonArray {
            for (t in req.tags) {
                val v = t.trim()
                if (v.isNotBlank()) add(JsonPrimitive(v))
            }
        }

        val sql = if (modelId == null) {
            """
            INSERT INTO prompt_store_prompts (
              id, mode_id, model_id, prompt_text, temperature, is_enabled, tags, origin, meta, raw
            ) VALUES (
              ?::uuid, ?, NULL, ?, ?, ?, ?::jsonb, ?::jsonb, ?::jsonb, ?::jsonb
            )
            ON CONFLICT (mode_id) WHERE model_id IS NULL
            DO UPDATE SET
              prompt_text = EXCLUDED.prompt_text,
              temperature = EXCLUDED.temperature,
              is_enabled = EXCLUDED.is_enabled,
              tags = EXCLUDED.tags,
              origin = EXCLUDED.origin,
              meta = EXCLUDED.meta,
              raw = EXCLUDED.raw,
              updated_at = now()
            RETURNING id::text, (xmax = 0) AS inserted
            """.trimIndent()
        } else {
            """
            INSERT INTO prompt_store_prompts (
              id, mode_id, model_id, prompt_text, temperature, is_enabled, tags, origin, meta, raw
            ) VALUES (
              ?::uuid, ?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, ?::jsonb, ?::jsonb
            )
            ON CONFLICT (mode_id, model_id) WHERE model_id IS NOT NULL
            DO UPDATE SET
              prompt_text = EXCLUDED.prompt_text,
              temperature = EXCLUDED.temperature,
              is_enabled = EXCLUDED.is_enabled,
              tags = EXCLUDED.tags,
              origin = EXCLUDED.origin,
              meta = EXCLUDED.meta,
              raw = EXCLUDED.raw,
              updated_at = now()
            RETURNING id::text, (xmax = 0) AS inserted
            """.trimIndent()
        }

        ds.connection.use { conn ->
            conn.prepareStatement(sql).use { ps ->
                var i = 1
                ps.setString(i++, newId)
                ps.setString(i++, modeId)
                if (modelId != null) ps.setString(i++, modelId)
                ps.setString(i++, req.promptText)
                ps.setDouble(i++, req.temperature)
                ps.setBoolean(i++, req.isEnabled)
                ps.setString(i++, toJsonb(tagsEl))
                ps.setString(i++, toJsonb(req.origin))
                ps.setString(i++, toJsonb(req.meta))
                ps.setString(i++, toJsonb(req.raw))
                ps.executeQuery().use { rs ->
                    if (!rs.next()) error("Upsert returned no rows")
                    val id = rs.getString(1)
                    val inserted = rs.getBoolean(2)
                    return UpsertResult(id = id, upserted = inserted)
                }
            }
        }
    }

    private fun toJsonb(el: JsonElement): String =
        json.encodeToString(JsonElement.serializer(), el)
}
