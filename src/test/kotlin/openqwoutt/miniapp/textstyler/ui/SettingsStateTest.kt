package openqwoutt.miniapp.textstyler.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for SettingsState and related data classes.
 */
class SettingsStateTest {

    @Test
    fun `custom backend at end when disabled`() {
        val state = SettingsState(customBackendEnabled = false)
        val items = buildMenuItems(state)
        assertEquals("Custom Backend", items.last().title)
    }

    @Test
    fun `custom backend first when enabled`() {
        val state = SettingsState(customBackendEnabled = true)
        val items = buildMenuItems(state)
        assertEquals("Custom Backend", items.first().title)
    }

    @Test
    fun `nvidia models list is hardcoded`() {
        val models = NvidiaModels.models
        assertEquals(2, models.size)
        assertTrue(models.contains("meta/llama-3.1-70b-instruct"))
        assertTrue(models.contains("qwen/qwen3-coder-480b-a35b-instruct"))
    }

    @Test
    fun `nvidia models display names are correct`() {
        assertEquals("Llama 3.1 70B Instruct", NvidiaModels.getDisplayName("meta/llama-3.1-70b-instruct"))
        assertEquals("Qwen3 Coder 480B", NvidiaModels.getDisplayName("qwen/qwen3-coder-480b-a35b-instruct"))
    }

    @Test
    fun `default ai model is llama`() {
        val state = SettingsState()
        assertEquals("meta/llama-3.1-70b-instruct", state.aiModel)
    }

    @Test
    fun `default language is English`() {
        val state = SettingsState()
        assertEquals("English", state.language)
    }

    @Test
    fun `custom backend disabled by default`() {
        val state = SettingsState()
        assertEquals(false, state.customBackendEnabled)
    }

    @Test
    fun `four menu items total`() {
        val state = SettingsState(customBackendEnabled = false)
        val items = buildMenuItems(state)
        assertEquals(4, items.size)
    }

    @Test
    fun `ai model menu item is always first`() {
        val stateEnabled = SettingsState(customBackendEnabled = true)
        val stateDisabled = SettingsState(customBackendEnabled = false)
        
        assertTrue(buildMenuItems(stateEnabled).first() is SettingsMenuItem.AiModel)
        assertTrue(buildMenuItems(stateDisabled).first() is SettingsMenuItem.AiModel)
    }

    @Test
    fun `language menu item is second when backend disabled`() {
        val state = SettingsState(customBackendEnabled = false)
        val items = buildMenuItems(state)
        assertTrue(items[1] is SettingsMenuItem.Language)
    }
}