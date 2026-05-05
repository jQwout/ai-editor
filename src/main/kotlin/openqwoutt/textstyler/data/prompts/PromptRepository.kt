package openqwoutt.miniapp.textstyler.data.prompts

import android.content.Context
import android.content.SharedPreferences

/**
 * Repository for AI prompt templates.
 */
class PromptRepository(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getTemplates(): List<PromptTemplate> = DEFAULT_PROMPTS

    fun getTemplatesByCategory(category: String): List<PromptTemplate> =
        DEFAULT_PROMPTS.filter { it.category == category }

    fun getTemplateById(id: String): PromptTemplate? =
        DEFAULT_PROMPTS.find { it.id == id }

    companion object {
        private const val PREFS_NAME = "prompt_templates"
    }
}