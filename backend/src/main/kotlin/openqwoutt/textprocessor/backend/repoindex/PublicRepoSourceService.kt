package openqwoutt.textprocessor.backend.repoindex

class PublicRepoSourceService(
    private val dao: PublicRepoSourceDao,
) {
    fun listEnabled(): List<PublicRepoSourceRow> = dao.listEnabled()

    fun upsertGithubRepos(repoUrls: List<String>) {
        for (u in repoUrls.map { it.trim() }.filter { it.isNotBlank() }.distinct()) {
            dao.upsert(repoUrl = u, provider = "github", isEnabled = true)
        }
    }

    fun markChecked(repoUrl: String, defaultBranch: String?, lastSeenCommit: String?) =
        dao.markChecked(repoUrl, defaultBranch, lastSeenCommit)

    fun markIngested(repoUrl: String, lastIngestedCommit: String?) =
        dao.markIngested(repoUrl, lastIngestedCommit)
}

