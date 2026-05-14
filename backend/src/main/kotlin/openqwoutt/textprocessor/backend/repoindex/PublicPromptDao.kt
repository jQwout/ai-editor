package openqwoutt.textprocessor.backend.repoindex

import java.sql.Connection
import javax.sql.DataSource

data class PublicPromptRow(
    val source: String,
    val path: String,
    val sha: String,
    val itemKey: String,
    val tags: List<String>,
    val promptText: String,
)

class PublicPromptDao(private val ds: DataSource) {

    fun upsertAll(rows: List<PublicPromptRow>): Int {
        if (rows.isEmpty()) return 0
        ds.connection.use { c ->
            c.autoCommit = false
            try {
                val total = rows.sumOf { upsertOne(c, it) }
                c.commit()
                return total
            } catch (t: Throwable) {
                runCatching { c.rollback() }
                throw t
            } finally {
                c.autoCommit = true
            }
        }
    }

    private fun upsertOne(c: Connection, row: PublicPromptRow): Int =
        c.prepareStatement(
            """
            insert into public_prompt (source, path, sha, item_key, tags, prompt_text, updated_at)
            values (?, ?, ?, ?, ?, ?, now())
            on conflict (source, path, sha, item_key) do update set
              tags = excluded.tags,
              prompt_text = excluded.prompt_text,
              updated_at = now()
            """.trimIndent()
        ).use { ps ->
            ps.setString(1, row.source)
            ps.setString(2, row.path)
            ps.setString(3, row.sha)
            ps.setString(4, row.itemKey)
            ps.setArray(5, c.createArrayOf("text", row.tags.toTypedArray()))
            ps.setString(6, row.promptText)
            ps.executeUpdate()
        }

    fun list(
        source: String?,
        tag: String?,
        limit: Int,
    ): List<PublicPromptRow> =
        ds.connection.use { c ->
            val clauses = buildList {
                if (!source.isNullOrBlank()) add("source = ?")
                if (!tag.isNullOrBlank()) add("? = any(tags)")
            }
            val where = if (clauses.isEmpty()) "" else "where " + clauses.joinToString(" and ")
            c.prepareStatement(
                """
                select source, path, sha, item_key, tags, prompt_text
                from public_prompt
                $where
                order by updated_at desc
                limit ?
                """.trimIndent()
            ).use { ps ->
                var i = 1
                if (!source.isNullOrBlank()) ps.setString(i++, source.trim())
                if (!tag.isNullOrBlank()) ps.setString(i++, tag.trim())
                ps.setInt(i, limit.coerceIn(1, 500))
                ps.executeQuery().use { rs ->
                    buildList {
                        while (rs.next()) {
                            val tagsArr = (rs.getArray("tags")?.array as? Array<*>)?.mapNotNull { it as? String }.orEmpty()
                            add(
                                PublicPromptRow(
                                    source = rs.getString("source"),
                                    path = rs.getString("path"),
                                    sha = rs.getString("sha"),
                                    itemKey = rs.getString("item_key"),
                                    tags = tagsArr,
                                    promptText = rs.getString("prompt_text"),
                                )
                            )
                        }
                    }
                }
            }
        }
}

