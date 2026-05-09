package openqwoutt.textstyler.network

import kotlinx.coroutines.runBlocking
import openqwoutt.textstyler.data.settings.AiProvider
import org.junit.Assume.assumeTrue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test

class AiApiClientTest {

    @Test
    fun nvidiaChatCompletion_withSimplePrompt_returnsNonEmptyContent() = runBlocking {
        val apiKey = System.getenv("NVIDIA_API_KEY")
        assumeTrue("NVIDIA_API_KEY not set", !apiKey.isNullOrBlank())
        val client = AiApiClient()

        val response = client.chatCompletion(
            provider = AiProvider.NVIDIA,
            model = "meta/llama-3.1-70b-instruct",
            messages = listOf(ChatMessage(role = "user", content = "Say hello in one word.")),
            apiKey = apiKey
        )

        val content = response.firstContent()
        assertNotNull(content)
        assertFalse(content!!.isBlank())
        println("NVIDIA simple response: $content")
    }

    @Test
    fun nvidiaChatCompletion_withTemperature_returnsNonEmptyContent() = runBlocking {
        val apiKey = System.getenv("NVIDIA_API_KEY")
        assumeTrue("NVIDIA_API_KEY not set", !apiKey.isNullOrBlank())
        val client = AiApiClient()

        val response = client.chatCompletion(
            provider = AiProvider.NVIDIA,
            model = "meta/llama-3.1-70b-instruct",
            messages = listOf(
                ChatMessage(
                    role = "user",
                    content = "Describe a sunset in one sentence."
                )
            ),
            apiKey = apiKey,
            temperature = 0.9
        )

        val content = response.firstContent()
        assertNotNull(content)
        assertFalse(content!!.isBlank())
        println("NVIDIA temperature response: $content")
    }

    @Test
    fun nvidiaChatCompletion_qwenCoder_returnsNonEmptyContent() = runBlocking {
        val apiKey = System.getenv("NVIDIA_API_KEY")
        assumeTrue("NVIDIA_API_KEY not set", !apiKey.isNullOrBlank())
        val client = AiApiClient()

        val response = client.chatCompletion(
            provider = AiProvider.NVIDIA,
            model = "qwen/qwen3-coder-480b-a35b-instruct",
            messages = listOf(
                ChatMessage(
                    role = "user",
                    content = "Write a Python function that returns all prime numbers below n."
                )
            ),
            apiKey = apiKey,
            temperature = 0.7,
            topP = 0.8,
            maxTokens = 4096
        )

        val content = response.firstContent()
        assertNotNull(content)
        assertFalse(content!!.isBlank())
        println("NVIDIA QwenCoder response: $content")
    }
}
