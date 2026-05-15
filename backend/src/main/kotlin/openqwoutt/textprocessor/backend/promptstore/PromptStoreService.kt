package openqwoutt.textprocessor.backend.promptstore

class PromptStoreService(
    private val dao: PromptStoreDao,
) {
    fun upsertPrompt(req: AdminUpsertPromptRequest): PromptStoreDao.UpsertResult =
        dao.upsertPrompt(req)

    fun upsertPromptBatch(requests: List<AdminUpsertPromptRequest>): List<PromptStoreDao.UpsertResult> =
        dao.upsertPromptBatch(requests)
}
