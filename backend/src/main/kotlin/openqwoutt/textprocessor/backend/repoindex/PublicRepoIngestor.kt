package openqwoutt.textprocessor.backend.repoindex

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Base64

class PublicRepoIngestor(
    private val http: HttpClient,
    private val cfg: RepoIndexConfig,
    private val publicPromptService: PublicPromptService,
) {
    suspend fun getGithubHeadCommit(repoUrl: String): HeadCommitResult {
        val (owner, repo) = parseGithubOwnerRepo(repoUrl)
            ?: return HeadCommitResult(repoUrl = repoUrl, defaultBranch = null, headCommitSha = null, error = "Unsupported repoUrl")

        val token = System.getenv("PUBLIC_PROMPT_GITHUB_TOKEN")?.trim()?.takeIf { it.isNotBlank() }

        val repoInfo = http.get("https://api.github.com/repos/$owner/$repo") {
            header(HttpHeaders.Accept, "application/vnd.github+json")
            if (token != null) header(HttpHeaders.Authorization, "Bearer $token")
        }.body<GhRepo>()

        val defaultBranch = repoInfo.defaultBranch
        val commit = http.get("https://api.github.com/repos/$owner/$repo/commits/$defaultBranch") {
            header(HttpHeaders.Accept, "application/vnd.github+json")
            if (token != null) header(HttpHeaders.Authorization, "Bearer $token")
        }.body<GhCommitRef>()

        return HeadCommitResult(repoUrl = repoUrl, defaultBranch = defaultBranch, headCommitSha = commit.sha, error = null)
    }

    suspend fun ingestGithubRepo(repoUrl: String): IngestResult {
        val (owner, repo) = parseGithubOwnerRepo(repoUrl)
            ?: return IngestResult(repoUrl = repoUrl, filesSeen = 0, promptsUpserted = 0, error = "Unsupported repoUrl")

        val token = System.getenv("PUBLIC_PROMPT_GITHUB_TOKEN")?.trim()?.takeIf { it.isNotBlank() }

        val repoInfo = http.get("https://api.github.com/repos/$owner/$repo") {
            header(HttpHeaders.Accept, "application/vnd.github+json")
            if (token != null) header(HttpHeaders.Authorization, "Bearer $token")
        }.body<GhRepo>()

        val defaultBranch = repoInfo.defaultBranch

        val tree = http.get("https://api.github.com/repos/$owner/$repo/git/trees/$defaultBranch?recursive=1") {
            header(HttpHeaders.Accept, "application/vnd.github+json")
            if (token != null) header(HttpHeaders.Authorization, "Bearer $token")
        }.body<GhTreeResponse>()

        val blobs = tree.tree
            .asSequence()
            .filter { it.type == "blob" }
            .filter { isSupportedPromptPath(it.path) }
            .toList()

        var prompts = 0
        var filesSeen = 0

        val rows = buildList {
            for (b in blobs) {
                val blobSha = b.sha ?: continue
                val size = b.size ?: 0
                if (size <= 0 || size > 512_000) continue // avoid huge files

                val blob = http.get("https://api.github.com/repos/$owner/$repo/git/blobs/$blobSha") {
                    header(HttpHeaders.Accept, "application/vnd.github+json")
                    if (token != null) header(HttpHeaders.Authorization, "Bearer $token")
                }.body<GhBlob>()

                val content = decodeBlobContent(blob) ?: continue
                filesSeen++

                val parsed = PublicPromptParser.parseByPath(b.path, content)
                if (parsed.isEmpty()) continue

                parsed.forEachIndexed { idx, p ->
                    val itemKey = p.title?.takeIf { it.isNotBlank() } ?: idx.toString()
                    add(
                        PublicPromptRow(
                            source = repoUrl.trim(),
                            path = b.path,
                            sha = blobSha,
                            itemKey = itemKey,
                            tags = p.tags,
                            promptText = p.promptText,
                        )
                    )
                    prompts++
                }
            }
        }

        val upserted = publicPromptService.upsertPrompts(rows)
        return IngestResult(repoUrl = repoUrl, filesSeen = filesSeen, promptsUpserted = upserted, error = null)
    }

    private fun isSupportedPromptPath(path: String): Boolean {
        val lower = path.lowercase()
        return lower.endsWith(".md") || lower.endsWith(".markdown") || lower.endsWith(".csv")
    }

    private fun decodeBlobContent(blob: GhBlob): String? {
        val raw = blob.content ?: return null
        val encoding = blob.encoding ?: "base64"
        if (encoding != "base64") return null
        val cleaned = raw.replace("\n", "").replace("\r", "")
        val bytes = runCatching { Base64.getDecoder().decode(cleaned) }.getOrNull() ?: return null
        return bytes.toString(Charsets.UTF_8)
    }

    private fun parseGithubOwnerRepo(repoUrl: String): Pair<String, String>? {
        val u = repoUrl.trim()
        val m = Regex("""^https?://github\.com/([^/]+)/([^/]+?)(?:\.git)?/?$""", RegexOption.IGNORE_CASE)
            .matchEntire(u) ?: return null
        return m.groupValues[1] to m.groupValues[2]
    }
}

@Serializable
data class IngestResult(
    val repoUrl: String,
    val filesSeen: Int,
    val promptsUpserted: Int,
    val error: String?,
)

@Serializable
data class HeadCommitResult(
    val repoUrl: String,
    val defaultBranch: String?,
    val headCommitSha: String?,
    val error: String?,
)

@Serializable
private data class GhRepo(
    @SerialName("default_branch")
    val defaultBranch: String,
)

@Serializable
private data class GhTreeResponse(
    val tree: List<GhTreeEntry> = emptyList(),
)

@Serializable
private data class GhTreeEntry(
    val path: String,
    val type: String,
    val sha: String? = null,
    val size: Int? = null,
)

@Serializable
private data class GhBlob(
    val content: String? = null,
    val encoding: String? = null,
)

@Serializable
private data class GhCommitRef(
    val sha: String,
)

