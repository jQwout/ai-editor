package openqwoutt.textprocessor.backend.textprocessing.infrastructure

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class OpenRouterConnectionConfigTest {

    @Test
    fun `fromEnvironment uses defaults`() {
        val env = mapOf("OPENROUTER_API_KEY" to "sk-test")
        val c = OpenRouterConnectionConfig.fromEnvironment { name -> env[name] }
        assertEquals("sk-test", c.apiKey)
        assertEquals("openai/gpt-4o-mini", c.defaultRoutingModelId)
        assertEquals("Side AI Editor", c.appTitle)
        assertNull(c.httpReferer)
    }

    @Test
    fun `fromEnvironment reads all vars`() {
        val env =
            mapOf(
                "OPENROUTER_API_KEY" to "k",
                "OPENROUTER_MODEL" to "x/y",
                "OPENROUTER_APP_TITLE" to "T",
                "OPENROUTER_HTTP_REFERER" to "https://ref",
            )
        val c = OpenRouterConnectionConfig.fromEnvironment { name -> env[name] }
        assertEquals("x/y", c.defaultRoutingModelId)
        assertEquals("T", c.appTitle)
        assertEquals("https://ref", c.httpReferer)
    }

    @Test
    fun `fromEnvironment requires key`() {
        assertThrows<IllegalStateException> {
            OpenRouterConnectionConfig.fromEnvironment { null }
        }
    }
}
