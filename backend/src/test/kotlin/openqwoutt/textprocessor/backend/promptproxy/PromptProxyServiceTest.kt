package openqwoutt.textprocessor.backend.promptproxy

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import openqwoutt.textprocessor.backend.shared.openrouter.ChatMessage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PromptProxyServiceTest {

    private class RecordingCompleter(
        private val syncResult: LlmAttemptResult,
        private val streamFlow: Flow<ProxyStreamFrame> = emptyFlow(),
    ) : PromptProxyLlmCompleter {
        override suspend fun completeChat(
            model: String,
            messages: List<ChatMessage>,
            temperature: Double,
            maxTokens: Int,
        ): LlmAttemptResult = syncResult

        override fun streamChat(
            model: String,
            messages: List<ChatMessage>,
            temperature: Double,
            maxTokens: Int,
        ): Flow<ProxyStreamFrame> = streamFlow
    }

    @Test
    fun `run returns ok when llm succeeds`() =
        runBlocking {
            val svc =
                PromptProxyService(
                    RecordingCompleter(LlmAttemptResult.Success("OUT")),
                    defaultRoutingModel = "def",
                )
            val r =
                svc.run(
                    PromptProxyRequest(
                        style = "s",
                        prompt = "p",
                        language = "en",
                    ),
                )
            assertTrue(r is PromptProxyRunOutcome.Ok)
            assertEquals("OUT", (r as PromptProxyRunOutcome.Ok).text)
        }

    @Test
    fun `run returns validation when prompt blank`() =
        runBlocking {
            val svc =
                PromptProxyService(
                    RecordingCompleter(LlmAttemptResult.Success("x")),
                    defaultRoutingModel = "def",
                )
            val r =
                svc.run(
                    PromptProxyRequest(
                        style = "",
                        prompt = "  ",
                        language = "en",
                    ),
                )
            assertTrue(r is PromptProxyRunOutcome.Validation)
        }

    @Test
    fun `streamPrepare returns validation when model missing and no default`() =
        runBlocking {
            val svc =
                PromptProxyService(
                    RecordingCompleter(LlmAttemptResult.Success("x")),
                    defaultRoutingModel = "",
                )
            val prep =
                svc.streamPrepare(
                    PromptProxyRequest(
                        style = "",
                        prompt = "hi",
                        language = "en",
                    ),
                )
            assertTrue(prep is PromptProxyStreamPrepare.Validation)
            assertTrue(
                (prep as PromptProxyStreamPrepare.Validation).detail.message.contains("model", ignoreCase = true),
            )
        }

    @Test
    fun `streamPrepare returns frames from llm`() =
        runBlocking {
            val flow = flowOf(ProxyStreamFrame.Delta("a"))
            val svc =
                PromptProxyService(
                    RecordingCompleter(LlmAttemptResult.Success("unused"), streamFlow = flow),
                    defaultRoutingModel = "dm",
                )
            val prep =
                svc.streamPrepare(
                    PromptProxyRequest(
                        style = "",
                        prompt = "x",
                        language = "en",
                        stream = true,
                    ),
                )
            assertTrue(prep is PromptProxyStreamPrepare.Frames)
            val frames =
                (prep as PromptProxyStreamPrepare.Frames).flow.toList()
            assertEquals(listOf(ProxyStreamFrame.Delta("a")), frames)
        }
}
