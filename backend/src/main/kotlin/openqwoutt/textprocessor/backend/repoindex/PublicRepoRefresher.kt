package openqwoutt.textprocessor.backend.repoindex

import io.ktor.server.application.Application
import io.ktor.server.application.log
import kotlinx.serialization.Serializable

class PublicRepoRefresher(
    private val repoSourceService: PublicRepoSourceService,
    private val ingestor: PublicRepoIngestor,
) {
    suspend fun refreshOnce(app: Application): List<RepoRefreshResult> {
        val repos = repoSourceService.listEnabled()
        if (repos.isEmpty()) return emptyList()

        return buildList {
            for (r in repos) {
                if (r.provider.lowercase() != "github") continue

                val head = runCatching { ingestor.getGithubHeadCommit(r.repoUrl) }.getOrElse {
                    app.log.warn("Head commit check failed for ${r.repoUrl}", it)
                    add(RepoRefreshResult(repoUrl = r.repoUrl, action = "head_check_failed", error = it.message))
                    null
                } ?: continue

                if (head.error != null) {
                    add(RepoRefreshResult(repoUrl = r.repoUrl, action = "head_check_error", error = head.error))
                    continue
                }

                repoSourceService.markChecked(
                    repoUrl = r.repoUrl,
                    defaultBranch = head.defaultBranch,
                    lastSeenCommit = head.headCommitSha,
                )

                val lastSeen = head.headCommitSha
                if (lastSeen.isNullOrBlank()) {
                    add(RepoRefreshResult(repoUrl = r.repoUrl, action = "no_head_commit", error = null))
                    continue
                }

                if (lastSeen == r.lastIngestedCommit) {
                    add(RepoRefreshResult(repoUrl = r.repoUrl, action = "up_to_date", error = null))
                    continue
                }

                val res = runCatching { ingestor.ingestGithubRepo(r.repoUrl) }.getOrElse {
                    app.log.warn("Ingest failed for ${r.repoUrl}", it)
                    add(RepoRefreshResult(repoUrl = r.repoUrl, action = "ingest_failed", error = it.message))
                    null
                } ?: continue

                if (res.error != null) {
                    add(RepoRefreshResult(repoUrl = r.repoUrl, action = "ingest_error", error = res.error))
                    continue
                }

                repoSourceService.markIngested(repoUrl = r.repoUrl, lastIngestedCommit = lastSeen)
                add(
                    RepoRefreshResult(
                        repoUrl = r.repoUrl,
                        action = "ingested",
                        filesSeen = res.filesSeen,
                        promptsUpserted = res.promptsUpserted,
                        error = null,
                    )
                )
            }
        }
    }
}

@Serializable
data class RepoRefreshResult(
    val repoUrl: String,
    /** up_to_date | ingested | head_check_* | ingest_* */
    val action: String,
    val filesSeen: Int? = null,
    val promptsUpserted: Int? = null,
    val error: String? = null,
)

