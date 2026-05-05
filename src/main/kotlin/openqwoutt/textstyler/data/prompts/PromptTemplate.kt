package openqwoutt.miniapp.textstyler.data.prompts

/**
 * Prompt template for AI processing.
 * @param id Unique identifier
 * @param name Display name
 * @param template Prompt template to apply
 * @param category Category
 * @param description Description
 */
data class PromptTemplate(
    val id: String,
    val name: String,
    val template: String,
    val category: String,
    val description: String = ""
)

object PromptCategory {
    const val TRANSFORM = "transform"  // shorten, expand, etc
    const val ANALYSIS = "analysis"  // summarize, explain
    const val CREATE = "create"    // examples, variations
    const val FORMAT = "format"  // bullet points, tables
}

/**
 * Hardcoded AI prompt templates for daily use.
 */
val DEFAULT_PROMPTS = listOf(
    // Transform
    PromptTemplate(
        id = "shorten",
        name = "Shorten",
        template = "Make this shorter while keeping the key meaning:\n",
        category = PromptCategory.TRANSFORM,
        description = "Condense text"
    ),
    PromptTemplate(
        id = "expand",
        name = "Expand",
        template = "Expand this with more detail and context:\n",
        category = PromptCategory.TRANSFORM,
        description = "Add more detail"
    ),
    PromptTemplate(
        id = "simplify",
        name = "Simplify",
        template = "Simplify this text, use basic words:\n",
        category = PromptCategory.TRANSFORM,
        description = "Make simple"
    ),
    PromptTemplate(
        id = "formal",
        name = "Formal",
        template = "Rewrite in formal professional style:\n",
        category = PromptCategory.TRANSFORM,
        description = "Formal tone"
    ),
    PromptTemplate(
        id = "casual",
        name = "Casual",
        template = "Rewrite in casual conversational style:\n",
        category = PromptCategory.TRANSFORM,
        description = "Casual tone"
    ),

    // Analysis
    PromptTemplate(
        id = "summarize",
        name = "Summarize",
        template = "Summarize the key points:\n",
        category = PromptCategory.ANALYSIS,
        description = "Short summary"
    ),
    PromptTemplate(
        id = "explain",
        name = "Explain",
        template = "Explain this in simple terms:\n",
        category = PromptCategory.ANALYSIS,
        description = "Simple explanation"
    ),
    PromptTemplate(
        id = "pros_cons",
        name = "Pros & Cons",
        template = "List the pros and cons:\n",
        category = PromptCategory.ANALYSIS,
        description = "Advantages/disadvantages"
    ),
    PromptTemplate(
        id = "key_points",
        name = "Key Points",
        template = "Extract the main ideas:\n",
        category = PromptCategory.ANALYSIS,
        description = "Main ideas"
    ),

    // Create
    PromptTemplate(
        id = "examples",
        name = "Add Examples",
        template = "Add practical examples:\n",
        category = PromptCategory.CREATE,
        description = "Add examples"
    ),
    PromptTemplate(
        id = "variations",
        name = "Variations",
        template = "Create variations of this text:\n",
        category = PromptCategory.CREATE,
        description = "Different versions"
    ),
    PromptTemplate(
        id = "questions",
        name = "FAQs",
        template = "Generate relevant questions and answers:\n",
        category = PromptCategory.CREATE,
        description = "Create Q&A"
    ),

    // Format
    PromptTemplate(
        id = "bullets",
        name = "Bullet Points",
        template = "Convert to bullet points:\n",
        category = PromptCategory.FORMAT,
        description = "As a list"
    ),
    PromptTemplate(
        id = "steps",
        name = "Steps",
        template = "Convert to numbered steps:\n",
        category = PromptCategory.FORMAT,
        description = "As instructions"
    ),
    PromptTemplate(
        id = "table",
        name = "Table",
        template = "Format as a table:\n",
        category = PromptCategory.FORMAT,
        description = "As table"
    )
)