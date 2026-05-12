package openqwoutt.textprocessor.backend.promptstore

data class PromptStoreConfig(
    val postgresJdbcUrl: String,
    val postgresUser: String,
    val postgresPassword: String,
    /** If set, required as `Authorization: Bearer <token>` for admin routes */
    val adminToken: String?,
) {
    companion object {
        fun fromEnv(): PromptStoreConfig =
            PromptStoreConfig(
                postgresJdbcUrl = env("PROMPT_DB_JDBC_URL"),
                postgresUser = env("PROMPT_DB_USER"),
                postgresPassword = env("PROMPT_DB_PASSWORD"),
                adminToken = System.getenv("PROMPT_ADMIN_TOKEN")?.trim()?.takeIf { it.isNotBlank() },
            )

        private fun env(name: String): String =
            System.getenv(name)?.takeIf { it.isNotBlank() }
                ?: error("Missing env $name")
    }
}
