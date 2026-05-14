package openqwoutt.textprocessor.backend.promptproxy

import io.ktor.client.HttpClient
import io.micrometer.core.instrument.MeterRegistry
import io.ktor.server.application.Application
import io.ktor.server.application.log
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json

object PromptProxyFeature {
    /**
     * @param routingModel Fallback model id when the client omits `model` in JSON (env / provider default).
     * May be empty if every client request supplies [PromptProxyRequest.model].
     */
    fun install(
        application: Application,
        json: Json,
        llmHttpClient: HttpClient?,
        chatCompletionsUrl: String?,
        providerId: String?,
        routingModel: String?,
        promptProxyApiKey: String?,
        meterRegistry: MeterRegistry? = null,
    ) {
        if (llmHttpClient == null || chatCompletionsUrl.isNullOrBlank() || providerId.isNullOrBlank()) {
            application.log.info(
                "Prompt proxy disabled (LLM HTTP client unavailable, chat URL missing, or provider unset)."
            )
            return
        }

        val apiKey = promptProxyApiKey?.trim().orEmpty()
        if (apiKey.isEmpty()) {
            application.log.warn(
                "Prompt proxy disabled: set PROMPT_PROXY_API_KEY when LLM is enabled (required for /api/prompt/proxy)."
            )
            return
        }

        val completer =
            ChatCompletionsPromptProxyCompleter(
                httpClient = llmHttpClient,
                json = json,
                chatCompletionsUrl = chatCompletionsUrl.trim(),
                providerId = providerId.trim(),
                meterRegistry = meterRegistry,
            )
        val defaultRoutingModel = routingModel?.trim().orEmpty()
        val service = PromptProxyService(llm = completer, defaultRoutingModel = defaultRoutingModel)

        application.routing {
            promptProxyRoutes(service, apiKey)
        }
    }
}
