package openqwoutt.textprocessor.backend.textprocessing.application

import kotlinx.coroutines.runBlocking
import openqwoutt.textprocessor.backend.textprocessing.domain.LlmCompletionException
import openqwoutt.textprocessor.backend.textprocessing.domain.StyleMode
import openqwoutt.textprocessor.backend.textprocessing.domain.TextProcessingPolicy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class ProcessTextUseCaseTest {

    private class FixedLlm(private val text: String) : LlmCompletionPort {
        override suspend fun complete(
            userText: String,
            taskPrompt: String,
            temperature: Double,
            routingModelId: String,
        ): String = text
    }

    private class ThrowingLlm(private val t: Throwable) : LlmCompletionPort {
        override suspend fun complete(
            userText: String,
            taskPrompt: String,
            temperature: Double,
            routingModelId: String,
        ): String {
            throw t
        }
    }

    private class RecordingLlm(private val text: String) : LlmCompletionPort {
        var lastUserText: String? = null
        var lastTaskPrompt: String? = null
        var lastRoutingModelId: String? = null

        override suspend fun complete(
            userText: String,
            taskPrompt: String,
            temperature: Double,
            routingModelId: String,
        ): String {
            lastUserText = userText
            lastTaskPrompt = taskPrompt
            lastRoutingModelId = routingModelId
            return text
        }
    }

    private val catalog =
        ModelPromptCatalog { modelId, modeId ->
            if (modelId == "m" && modeId == "style") {
                RegistryResolvedPrompt(promptText = "override-prompt", temperature = 0.9)
            } else {
                null
            }
        }

    @Test
    fun `disabled when llm null`() =
        runBlocking {
            val uc = ProcessTextUseCase(llm = null, modelPromptCatalog = null, fallbackRoutingModelId = "x")
            val r = uc.execute(ProcessTextCommand("hi", "style", null, false))
            assertTrue(r is ProcessTextOutcome.Err)
            r as ProcessTextOutcome.Err
            assertEquals(ProcessTextErrorKind.OPENROUTER_DISABLED, r.kind)
        }

    @Test
    fun `bad request blank text`() =
        runBlocking {
            val uc = ProcessTextUseCase(FixedLlm("x"), null, "m")
            val r = uc.execute(ProcessTextCommand("  \t ", "style", null, false))
            assertEquals(ProcessTextErrorKind.BAD_REQUEST, (r as ProcessTextOutcome.Err).kind)
        }

    @Test
    fun `unknown mode`() =
        runBlocking {
            val uc = ProcessTextUseCase(FixedLlm("x"), null, "m")
            val r = uc.execute(ProcessTextCommand("hi", "no_such_mode", null, false))
            assertEquals(ProcessTextErrorKind.UNKNOWN_MODE, (r as ProcessTextOutcome.Err).kind)
        }

    @Test
    fun `registry required when model set but no catalog`() =
        runBlocking {
            val uc = ProcessTextUseCase(FixedLlm("x"), null, "m")
            val r = uc.execute(ProcessTextCommand("hi", "style", "m", false))
            assertEquals(ProcessTextErrorKind.REGISTRY_REQUIRED, (r as ProcessTextOutcome.Err).kind)
        }

    @Test
    fun `model prompt not found`() =
        runBlocking {
            val uc = ProcessTextUseCase(FixedLlm("x"), catalog, "fallback")
            val r = uc.execute(ProcessTextCommand("hi", "style", "unknown-model", false))
            assertEquals(ProcessTextErrorKind.MODEL_PROMPT_NOT_FOUND, (r as ProcessTextOutcome.Err).kind)
        }

    @Test
    fun `success with explicit model uses catalog`() =
        runBlocking {
            val uc = ProcessTextUseCase(FixedLlm("OUT"), catalog, "fallback")
            val r = uc.execute(ProcessTextCommand(" hi ", "style", "m", false))
            assertTrue(r is ProcessTextOutcome.Ok)
            r as ProcessTextOutcome.Ok
            assertEquals("OUT", r.value.result)
            assertNull(r.value.routingModelId)
        }

    @Test
    fun `success without model uses fallback id and can return prompt details`() =
        runBlocking {
            val uc = ProcessTextUseCase(FixedLlm("OK"), null, "fallback-model")
            val r = uc.execute(ProcessTextCommand("text", "style", null, true))
            assertTrue(r is ProcessTextOutcome.Ok)
            r as ProcessTextOutcome.Ok
            assertEquals("OK", r.value.result)
            assertEquals("fallback-model", r.value.routingModelId)
            assertEquals("style", r.value.modeId)
            assertTrue(r.value.promptText!!.contains("Rewrite"))
            assertEquals(0.4, r.value.temperature!!, 0.001)
        }

    @Test
    fun `llm completion exception maps to err with provider fields`() =
        runBlocking {
            val uc =
                ProcessTextUseCase(
                    ThrowingLlm(
                        LlmCompletionException(
                            message = "fail-msg",
                            httpStatus = 502,
                            providerRaw = """{"e":1}""",
                            providerId = "nvidia",
                        ),
                    ),
                    null,
                    "m",
                )
            val r = uc.execute(ProcessTextCommand("hi", "style", null, false))
            assertTrue(r is ProcessTextOutcome.Err)
            r as ProcessTextOutcome.Err
            assertEquals(ProcessTextErrorKind.AI_BACKEND_FAILED, r.kind)
            assertEquals("fail-msg", r.message)
            assertEquals(502, r.httpStatus)
            assertEquals("nvidia", r.providerId)
            assertTrue(r.providerRaw!!.contains("e"))
        }

    @Test
    fun `generic exception maps to err with detail stack`() =
        runBlocking {
            val uc = ProcessTextUseCase(ThrowingLlm(RuntimeException("boom")), null, "m")
            val r = uc.execute(ProcessTextCommand("hi", "style", null, false))
            assertTrue(r is ProcessTextOutcome.Err)
            r as ProcessTextOutcome.Err
            assertEquals(ProcessTextErrorKind.AI_BACKEND_FAILED, r.kind)
            assertEquals("boom", r.message)
            assertTrue(r.detail!!.contains("RuntimeException"))
        }

    @Test
    fun `input longer than policy is clipped before llm`() =
        runBlocking {
            val llm = RecordingLlm("OK")
            val uc = ProcessTextUseCase(llm, null, "fb")
            val long = "a".repeat(TextProcessingPolicy.MAX_INPUT_CHARS + 400)
            val r = uc.execute(ProcessTextCommand(long, "style", null, false))
            assertTrue(r is ProcessTextOutcome.Ok)
            assertEquals(TextProcessingPolicy.MAX_INPUT_CHARS, llm.lastUserText!!.length)
            assertEquals("fb", llm.lastRoutingModelId)
        }

    @Test
    fun `summarize mode uses built in prompt`() =
        runBlocking {
            val llm = RecordingLlm("SUM")
            val uc = ProcessTextUseCase(llm, null, "fb")
            val r = uc.execute(ProcessTextCommand("long text", "summarize", null, false))
            assertTrue(r is ProcessTextOutcome.Ok)
            assertTrue(llm.lastTaskPrompt!!.contains("summary", ignoreCase = true))
            assertEquals("SUM", (r as ProcessTextOutcome.Ok).value.result)
        }

    @ParameterizedTest
    @EnumSource(StyleMode::class)
    fun `all enum modes are accepted`(mode: StyleMode) =
        runBlocking {
            val uc = ProcessTextUseCase(FixedLlm("ok"), null, "m")
            val r = uc.execute(ProcessTextCommand("x", mode.id, null, false))
            assertTrue(r is ProcessTextOutcome.Ok, "mode ${mode.id} should be valid")
        }

    @Test
    fun `explicit model uses routing id and registry prompt`() =
        runBlocking {
            val llm = RecordingLlm("R")
            val uc = ProcessTextUseCase(llm, catalog, "never-used-fallback")
            val r = uc.execute(ProcessTextCommand("body", "style", "m", true))
            assertTrue(r is ProcessTextOutcome.Ok)
            r as ProcessTextOutcome.Ok
            assertEquals("m", llm.lastRoutingModelId)
            assertEquals("override-prompt", llm.lastTaskPrompt)
            assertEquals("m", r.value.routingModelId)
            assertEquals("override-prompt", r.value.promptText)
        }
}
