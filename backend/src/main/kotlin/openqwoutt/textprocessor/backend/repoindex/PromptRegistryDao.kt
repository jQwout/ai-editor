package openqwoutt.textprocessor.backend.repoindex

import java.sql.ResultSet
import javax.sql.DataSource

data class AiModelRow(
    val id: Long,
    val modelId: String,
    val displayName: String,
    val isEnabled: Boolean,
)

data class PromptRow(
    val modeId: String,
    val promptText: String,
    val temperature: Double,
)

data class EffectivePromptRow(
    val promptText: String,
    val temperature: Double,
)

class PromptRegistryDao(private val ds: DataSource) {

    fun listEnabledModels(): List<AiModelRow> =
        ds.connection.use { c ->
            c.prepareStatement(
                """
                select id, model_id, display_name, is_enabled
                from ai_model
                where is_enabled = true
                order by display_name asc
                """.trimIndent()
            ).use { ps ->
                ps.executeQuery().use { rs ->
                    buildList {
                        while (rs.next()) add(mapAiModel(rs))
                    }
                }
            }
        }

    fun findEnabledModelByModelId(modelId: String): AiModelRow? =
        ds.connection.use { c ->
            c.prepareStatement(
                """
                select id, model_id, display_name, is_enabled
                from ai_model
                where model_id = ? and is_enabled = true
                """.trimIndent()
            ).use { ps ->
                ps.setString(1, modelId)
                ps.executeQuery().use { rs ->
                    if (!rs.next()) null else mapAiModel(rs)
                }
            }
        }

    fun resolveEffectivePrompt(modelIdStr: String, modeId: String): EffectivePromptRow? =
        ds.connection.use { c ->
            c.prepareStatement(
                """
                select
                  coalesce(mp.prompt_text_override, p.prompt_text) as prompt_text,
                  coalesce(mp.temperature_override, p.temperature) as temperature
                from ai_model m
                join prompt p on p.mode_id = ?
                left join model_prompt mp on mp.model_id = m.id and mp.mode_id = p.mode_id
                where m.model_id = ? and m.is_enabled = true
                """.trimIndent()
            ).use { ps ->
                ps.setString(1, modeId)
                ps.setString(2, modelIdStr)
                ps.executeQuery().use { rs ->
                    if (!rs.next()) null
                    else EffectivePromptRow(
                        promptText = rs.getString("prompt_text"),
                        temperature = rs.getDouble("temperature"),
                    )
                }
            }
        }

    fun listBasePrompts(): List<PromptRow> =
        ds.connection.use { c ->
            c.prepareStatement(
                """
                select mode_id, prompt_text, temperature
                from prompt
                order by mode_id asc
                """.trimIndent()
            ).use { ps ->
                ps.executeQuery().use { rs ->
                    buildList {
                        while (rs.next()) {
                            add(
                                PromptRow(
                                    modeId = rs.getString("mode_id"),
                                    promptText = rs.getString("prompt_text"),
                                    temperature = rs.getDouble("temperature"),
                                )
                            )
                        }
                    }
                }
            }
        }

    fun upsertModel(modelId: String, displayName: String, isEnabled: Boolean) {
        ds.connection.use { c ->
            c.prepareStatement(
                """
                insert into ai_model (model_id, display_name, is_enabled, updated_at)
                values (?, ?, ?, now())
                on conflict (model_id) do update set
                  display_name = excluded.display_name,
                  is_enabled = excluded.is_enabled,
                  updated_at = now()
                """.trimIndent()
            ).use { ps ->
                ps.setString(1, modelId)
                ps.setString(2, displayName)
                ps.setBoolean(3, isEnabled)
                ps.executeUpdate()
            }
        }
    }

    fun upsertBasePrompt(modeId: String, promptText: String, temperature: Double) {
        ds.connection.use { c ->
            c.prepareStatement(
                """
                insert into prompt (mode_id, prompt_text, temperature, updated_at)
                values (?, ?, ?, now())
                on conflict (mode_id) do update set
                  prompt_text = excluded.prompt_text,
                  temperature = excluded.temperature,
                  updated_at = now()
                """.trimIndent()
            ).use { ps ->
                ps.setString(1, modeId)
                ps.setString(2, promptText)
                ps.setDouble(3, temperature)
                ps.executeUpdate()
            }
        }
    }

    fun upsertModelPromptOverride(
        openRouterModelId: String,
        modeId: String,
        promptTextOverride: String?,
        temperatureOverride: Double?,
    ) {
        ds.connection.use { c ->
            c.prepareStatement(
                """
                insert into model_prompt (model_id, mode_id, prompt_text_override, temperature_override)
                select m.id, ?, ?, ?
                from ai_model m
                where m.model_id = ?
                on conflict (model_id, mode_id) do update set
                  prompt_text_override = excluded.prompt_text_override,
                  temperature_override = excluded.temperature_override
                """.trimIndent()
            ).use { ps ->
                ps.setString(1, modeId)
                if (promptTextOverride != null) ps.setString(2, promptTextOverride) else ps.setNull(2, java.sql.Types.VARCHAR)
                if (temperatureOverride != null) ps.setDouble(3, temperatureOverride) else ps.setNull(3, java.sql.Types.DOUBLE)
                ps.setString(4, openRouterModelId)
                ps.executeUpdate()
            }
        }
    }

    private fun mapAiModel(rs: ResultSet): AiModelRow =
        AiModelRow(
            id = rs.getLong("id"),
            modelId = rs.getString("model_id"),
            displayName = rs.getString("display_name"),
            isEnabled = rs.getBoolean("is_enabled"),
        )
}
