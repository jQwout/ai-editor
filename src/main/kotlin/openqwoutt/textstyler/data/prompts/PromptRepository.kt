package openqwoutt.miniapp.textstyler.data.prompts

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject

/**
 * Repository for AI prompt templates.
 * Loads from assets/prompts.json or falls back to hardcoded defaults.
 */
class PromptRepository(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Get all available prompt templates.
     */
    fun getTemplates(): List<PromptTemplate> = loadFromJson().ifEmpty { DEFAULT_PROMPTS }

    /**
     * Get templates by category.
     */
    fun getTemplatesByCategory(category: String): List<PromptTemplate> =
        getTemplates().filter { it.category == category }

    /**
     * Get template by ID.
     */
    fun getTemplateById(id: String): PromptTemplate? =
        getTemplates().find { it.id == id }

    /**
     * Search templates by query (searches name, description, tags).
     */
    fun search(query: String): List<PromptTemplate> {
        val q = query.lowercase().trim()
        if (q.isBlank()) return getTemplates()
        
        return getTemplates().filter { template ->
            template.name.lowercase().contains(q) ||
            template.description.lowercase().contains(q) ||
            template.tags.any { it.lowercase().contains(q) } ||
            template.prompt.lowercase().contains(q)
        }
    }

    /**
     * Get all categories.
     */
    fun getCategories(): List<PromptCategory> = listOf(
        PromptCategory("music", "Music", "Suno, Udio"),
        PromptCategory("video", "Video", "Sora, Kling, Veo"),
        PromptCategory("image", "Image", "Midjourney, DALL-E"),
        PromptCategory("text", "Text", "Text transformation")
    )

    private fun loadFromJson(): List<PromptTemplate> {
        return try {
            val json = context.assets.open("prompts.json").bufferedReader().use { it.readText() }
            val root = JSONObject(json)
            val categories = root.getJSONObject("categories")
            
            val templates = mutableListOf<PromptTemplate>()
            
            categories.keys().forEach { catKey ->
                val cat = categories.getJSONObject(catKey)
                val items = cat.getJSONArray("items")
                
                for (i in 0 until items.length()) {
                    val item = items.getJSONObject(i)
                    templates.add(
                        PromptTemplate(
                            id = item.getString("id"),
                            name = item.getString("name"),
                            prompt = item.getString("prompt"),
                            category = catKey,
                            description = item.optString("description", ""),
                            tags = item.optJSONArray("tags")?.let { arr ->
                                (0 until arr.length()).map { arr.getString(it) }
                            } ?: emptyList()
                        )
                    )
                }
            }
            templates
        } catch (e: Exception) {
            emptyList()
        }
    }

    companion object {
        private const val PREFS_NAME = "prompt_templates"
    }
}

/**
 * Category metadata.
 */
data class PromptCategory(
    val id: String,
    val name: String,
    val description: String
)