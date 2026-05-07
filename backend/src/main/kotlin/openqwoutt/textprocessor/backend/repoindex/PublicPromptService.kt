package openqwoutt.textprocessor.backend.repoindex

class PublicPromptService(
    private val dao: PublicPromptDao,
) {
    fun upsertPrompts(entries: List<PublicPromptRow>): Int = dao.upsertAll(entries)

    fun listPublicPrompts(
        source: String?,
        tag: String?,
        limit: Int,
    ): List<PublicPromptRow> = dao.list(source = source, tag = tag, limit = limit)
}

