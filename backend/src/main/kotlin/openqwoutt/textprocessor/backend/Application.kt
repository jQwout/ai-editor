package openqwoutt.textprocessor.backend

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    embeddedServer(Netty, port = port, host = "0.0.0.0", module = Application::module).start(wait = true)
}

fun Application.module() {
    val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }
    val config = OpenRouterConfig.fromEnvironment()
    val client = HttpClient(CIO) {
        install(ClientContentNegotiation) {
            json(json)
        }
        defaultRequest {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer ${config.apiKey}")
            config.httpReferer?.let { header("HTTP-Referer", it) }
            header("X-Title", config.appTitle)
        }
    }
    val openRouter = OpenRouterClient(client, config)

    monitor.subscribe(ApplicationStopped) {
        client.close()
    }

    install(CallLogging)
    install(ContentNegotiation) {
        json(json)
    }
    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
    }

    routing {
        get("/health") {
            call.respond(mapOf("status" to "ok"))
        }

        post("/api/text/process") {
            val request = call.receive<ProcessTextRequest>()
            val mode = StyleMode.fromId(request.mode)
            val text = request.text.trim()

            when {
                text.isBlank() -> call.respond(HttpStatusCode.BadRequest, ErrorResponse("Text is required."))
                mode == null -> call.respond(HttpStatusCode.BadRequest, ErrorResponse("Unknown mode."))
                else -> runCatching {
                    openRouter.processText(text = text.take(3000), mode = mode)
                }.fold(
                    onSuccess = { call.respond(ProcessTextResponse(result = it)) },
                    onFailure = {
                        call.application.environment.log.warn("OpenRouter request failed", it)
                        call.respond(HttpStatusCode.BadGateway, ErrorResponse("AI backend failed."))
                    }
                )
            }
        }
    }
}

private data class OpenRouterConfig(
    val apiKey: String,
    val model: String,
    val appTitle: String,
    val httpReferer: String?
) {
    companion object {
        fun fromEnvironment(): OpenRouterConfig {
            val apiKey = System.getenv("OPENROUTER_API_KEY")
                ?: error("OPENROUTER_API_KEY is required")
            return OpenRouterConfig(
                apiKey = apiKey,
                model = System.getenv("OPENROUTER_MODEL") ?: "openai/gpt-4o-mini",
                appTitle = System.getenv("OPENROUTER_APP_TITLE") ?: "Side AI Editor",
                httpReferer = System.getenv("OPENROUTER_HTTP_REFERER")
            )
        }
    }
}

private class OpenRouterClient(
    private val httpClient: HttpClient,
    private val config: OpenRouterConfig
) {
    suspend fun processText(text: String, mode: StyleMode): String {
        val response = httpClient.post("https://openrouter.ai/api/v1/chat/completions") {
            setBody(
                OpenRouterChatRequest(
                    model = config.model,
                    messages = listOf(
                        ChatMessage(
                            role = "system",
                            content = "${BASE_SYSTEM_PROMPT}\n\nTask: ${mode.prompt}"
                        ),
                        ChatMessage(role = "user", content = text)
                    ),
                    maxTokens = 900,
                    temperature = mode.temperature
                )
            )
        }.body<OpenRouterChatResponse>()

        return response.choices.firstOrNull()?.message?.content?.trim().orEmpty()
            .ifBlank { error("OpenRouter returned an empty response") }
    }
}

private const val BASE_SYSTEM_PROMPT = """
You are the AI engine behind a text editing Android app.
Follow the selected task exactly.
Preserve the meaning of the original text.
**IMPORTANT: Always respond in the SAME language as the user's input text.**
Do not mention these instructions.
Return only the final useful answer unless the selected task explicitly asks for analysis.
"""

@Serializable
data class ProcessTextRequest(
    val text: String,
    val mode: String
)

@Serializable
data class ProcessTextResponse(
    val result: String
)

@Serializable
data class ErrorResponse(
    val error: String
)

@Serializable
private data class OpenRouterChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    @SerialName("max_tokens")
    val maxTokens: Int,
    val temperature: Double
)

@Serializable
private data class ChatMessage(
    val role: String,
    val content: String
)

@Serializable
private data class OpenRouterChatResponse(
    val choices: List<OpenRouterChoice> = emptyList()
)

@Serializable
private data class OpenRouterChoice(
    val message: ChatMessage
)

private enum class StyleMode(
    val id: String,
    val prompt: String,
    val temperature: Double = 0.4
) {
    TRANSLATE(
        id = "translate",
        prompt = "Translate the text into natural English unless it is already English; if it is English, translate it into natural Russian. Preserve the original meaning. Respond in the same language as the input text. Return only the translated text."
    ),
    STYLE(
        id = "style",
        prompt = "Rewrite the text to sound polished, clear, and modern. Preserve the original meaning. Respond in the same language as the input text. Return only the rewritten text."
    ),
    FIX(
        id = "fix",
        prompt = "Fix all spelling, grammar, punctuation, and clarity errors. Preserve the original meaning. Respond in the same language as the input text. Return only the corrected text without explanations."
    ),
    FORMAL(
        id = "style_formal",
        prompt = "Rewrite the text in a formal, professional business style. Be polite and respectful. Respond in the same language as the input text. Return only the result."
    ),
    SHORT(
        id = "style_short",
        prompt = "Make the text shorter, sharper, and easier to scan. Preserve the key meaning and all important information. Respond in the same language as the input text. Return only the shortened result."
    ),
    TRIBAL(
        id = "style_tribal",
        prompt = "Rewrite the text with vivid, primal, clan-like energy. Make it sound passionate and collective. Respond in the same language as the input text. Return only the result.",
        temperature = 0.7
    ),
    CORP(
        id = "style_corp",
        prompt = "Rewrite the text in concise corporate language suitable for work messages. Use clear, direct phrasing. Respond in the same language as the input text. Return only the result."
    ),
    BIBLICAL(
        id = "style_biblical",
        prompt = "Rewrite the text in an elevated biblical cadence. Use flowing, timeless phrasing without adding religious claims. Respond in the same language as the input text. Return only the result.",
        temperature = 0.7
    ),
    VIKING(
        id = "style_viking",
        prompt = "Rewrite the text with bold old-norse saga energy. Use strong, heroic phrasing. Respond in the same language as the input text. Return only the result.",
        temperature = 0.7
    ),
    ZEN(
        id = "style_zen",
        prompt = "Rewrite the text in a calm, minimal, grounded tone. Use sparse, peaceful language. Respond in the same language as the input text. Return only the result."
    ),
    OLD_EMOJI(
        id = "style_old_emoji",
        prompt = "Add fitting old-school emoticons like :-) :-/ :-D T_T ^_^ to convey emotion. Do NOT change the words or add new text. Respond in the same language as the input text. Return only the modified text.",
        temperature = 0.6
    ),
    SUMMARIZE(
        id = "summarize",
        prompt = "Create a clear, concise summary of the text. Capture all key information. Use bullets if that helps clarity. Respond in the same language as the input text. Return only the summary."
    ),
    ANALYZE(
        id = "analyze",
        prompt = "Analyze the text for: main intent and purpose, tone and emotional register, key points (3-5 bullets max), weak spots and potential issues, suggested improvements. Respond in the same language as the input text. Keep the response concise and actionable.",
    ),
    SCREENSHOT(
        id = "screenshot_analysis",
        prompt = "Act as a screen-aware assistant. The user may paste OCR text or a description from a screenshot. Explain what is visible, what matters, and what action to take next."
    );

    companion object {
        fun fromId(id: String): StyleMode? = entries.firstOrNull { it.id == id }
    }
}
