package openqwoutt.textprocessor.backend.textprocessing.infrastructure

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class NvidiaConnectionConfigTest {

    @Test
    fun `fromEnvironment uses defaults when optional vars missing`() {
        val env =
            mapOf(
                "NVIDIA_API_KEY" to "k",
            )
        val c = NvidiaConnectionConfig.fromEnvironment { name -> env[name] }
        assertEquals("k", c.apiKey)
        assertEquals(NvidiaConnectionConfig.DEFAULT_CHAT_URL, c.chatCompletionsUrl)
        assertEquals("meta/llama-3.1-8b-instruct", c.defaultModelId)
    }

    @Test
    fun `fromEnvironment reads overrides`() {
        val env =
            mapOf(
                "NVIDIA_API_KEY" to "secret",
                "NVIDIA_CHAT_COMPLETIONS_URL" to " https://custom/v1/chat/completions ",
                "NVIDIA_MODEL" to " my/model ",
            )
        val c = NvidiaConnectionConfig.fromEnvironment { name -> env[name] }
        assertEquals("secret", c.apiKey)
        assertEquals("https://custom/v1/chat/completions", c.chatCompletionsUrl)
        assertEquals("my/model", c.defaultModelId)
    }

    @Test
    fun `fromEnvironment requires api key`() {
        assertThrows<IllegalStateException> {
            NvidiaConnectionConfig.fromEnvironment { null }
        }
    }
}
