package openqwoutt.textprocessor.backend.repoindex

import java.sql.ResultSet
import javax.sql.DataSource

data class PublicRepoSourceRow(
    val id: Long,
    val repoUrl: String,
    val provider: String,
    val defaultBranch: String?,
    val lastSeenCommit: String?,
    val lastIngestedCommit: String?,
    val isEnabled: Boolean,
)

class PublicRepoSourceDao(private val ds: DataSource) {

    fun listEnabled(): List<PublicRepoSourceRow> =
        ds.connection.use { c ->
            c.prepareStatement(
                """
                select id, repo_url, provider, default_branch, last_seen_commit, last_ingested_commit, is_enabled
                from public_repo_source
                where is_enabled = true
                order by updated_at desc
                """.trimIndent()
            ).use { ps ->
                ps.executeQuery().use { rs ->
                    buildList {
                        while (rs.next()) add(map(rs))
                    }
                }
            }
        }

    fun upsert(repoUrl: String, provider: String, isEnabled: Boolean) {
        ds.connection.use { c ->
            c.prepareStatement(
                """
                insert into public_repo_source (repo_url, provider, is_enabled, updated_at)
                values (?, ?, ?, now())
                on conflict (repo_url) do update set
                  provider = excluded.provider,
                  is_enabled = excluded.is_enabled,
                  updated_at = now()
                """.trimIndent()
            ).use { ps ->
                ps.setString(1, repoUrl)
                ps.setString(2, provider)
                ps.setBoolean(3, isEnabled)
                ps.executeUpdate()
            }
        }
    }

    fun markChecked(repoUrl: String, defaultBranch: String?, lastSeenCommit: String?) {
        ds.connection.use { c ->
            c.prepareStatement(
                """
                update public_repo_source
                set default_branch = coalesce(?, default_branch),
                    last_seen_commit = ?,
                    last_checked_at = now(),
                    updated_at = now()
                where repo_url = ?
                """.trimIndent()
            ).use { ps ->
                if (defaultBranch != null) ps.setString(1, defaultBranch) else ps.setNull(1, java.sql.Types.VARCHAR)
                if (lastSeenCommit != null) ps.setString(2, lastSeenCommit) else ps.setNull(2, java.sql.Types.VARCHAR)
                ps.setString(3, repoUrl)
                ps.executeUpdate()
            }
        }
    }

    fun markIngested(repoUrl: String, lastIngestedCommit: String?) {
        ds.connection.use { c ->
            c.prepareStatement(
                """
                update public_repo_source
                set last_ingested_commit = ?,
                    last_ingested_at = now(),
                    updated_at = now()
                where repo_url = ?
                """.trimIndent()
            ).use { ps ->
                if (lastIngestedCommit != null) ps.setString(1, lastIngestedCommit) else ps.setNull(1, java.sql.Types.VARCHAR)
                ps.setString(2, repoUrl)
                ps.executeUpdate()
            }
        }
    }

    private fun map(rs: ResultSet): PublicRepoSourceRow =
        PublicRepoSourceRow(
            id = rs.getLong("id"),
            repoUrl = rs.getString("repo_url"),
            provider = rs.getString("provider"),
            defaultBranch = rs.getString("default_branch"),
            lastSeenCommit = rs.getString("last_seen_commit"),
            lastIngestedCommit = rs.getString("last_ingested_commit"),
            isEnabled = rs.getBoolean("is_enabled"),
        )
}

