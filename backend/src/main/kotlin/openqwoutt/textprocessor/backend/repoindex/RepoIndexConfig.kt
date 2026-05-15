package openqwoutt.textprocessor.backend.repoindex

data class RepoIndexConfig(
    val postgresJdbcUrl: String,
    val postgresUser: String,
    val postgresPassword: String,
    /** If set, required as `Authorization: Bearer <token>` for admin routes */
    val adminToken: String?,
) {
    companion object {
        fun fromEnv(): RepoIndexConfig =
            RepoIndexConfig(
                postgresJdbcUrl = env("PG_JDBC_URL"),
                postgresUser = env("PG_USER"),
                postgresPassword = env("PG_PASSWORD"),
                adminToken = System.getenv("REPO_INDEX_ADMIN_TOKEN")?.trim()?.takeIf { it.isNotBlank() },
            )

        private fun env(name: String): String =
            System.getenv(name)?.takeIf { it.isNotBlank() }
                ?: error("Missing env $name")
    }
}
