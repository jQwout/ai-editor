package openqwoutt.textprocessor.backend.modelscatalog

const val MAX_CATALOG_MODEL_ID_LENGTH: Int = 256
const val MAX_CATALOG_DISPLAY_NAME_LENGTH: Int = 512

/**
 * Validates and normalizes a sync batch. Rejects blank fields, oversize strings, and duplicate [CatalogModelEntry.modelId].
 */
fun validateCatalogModelBatch(models: List<CatalogModelEntry>): List<CatalogModelEntry> {
    require(models.isNotEmpty()) { "models must not be empty" }

    val seen = LinkedHashSet<String>(models.size)
    val out = ArrayList<CatalogModelEntry>(models.size)

    models.forEachIndexed { index, raw ->
        val modelId = raw.modelId.trim()
        val displayName = raw.displayName.trim()

        when {
            modelId.isEmpty() ->
                throw IllegalArgumentException("models[$index].modelId must not be blank")
            modelId.length > MAX_CATALOG_MODEL_ID_LENGTH ->
                throw IllegalArgumentException(
                    "models[$index].modelId exceeds maximum length ($MAX_CATALOG_MODEL_ID_LENGTH)",
                )
            displayName.isEmpty() ->
                throw IllegalArgumentException("models[$index].displayName must not be blank")
            displayName.length > MAX_CATALOG_DISPLAY_NAME_LENGTH ->
                throw IllegalArgumentException(
                    "models[$index].displayName exceeds maximum length ($MAX_CATALOG_DISPLAY_NAME_LENGTH)",
                )
            !seen.add(modelId) ->
                throw IllegalArgumentException("duplicate modelId in batch: $modelId")
        }

        out.add(
            CatalogModelEntry(
                modelId = modelId,
                displayName = displayName,
                isEnabled = raw.isEnabled,
            ),
        )
    }

    return out
}
