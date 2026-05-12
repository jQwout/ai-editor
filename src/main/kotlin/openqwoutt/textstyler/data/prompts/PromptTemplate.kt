package openqwoutt.miniapp.textstyler.data.prompts

/**
 * AI prompt template for processing.
 */
data class PromptTemplate(
    val id: String,
    val name: String,
    val prompt: String,
    val category: String,
    val description: String = "",
    val tags: List<String> = emptyList()
)
