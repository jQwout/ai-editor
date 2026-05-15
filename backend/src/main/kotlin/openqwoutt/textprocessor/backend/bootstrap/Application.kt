package openqwoutt.textprocessor.backend.bootstrap

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import kotlinx.serialization.json.Json
import openqwoutt.textprocessor.backend.composition.PromptRegistryCatalogAdapter
import openqwoutt.textprocessor.backend.nvidiaproxy.NvidiaProxyFeature
import openqwoutt.textprocessor.backend.promptproxy.PromptProxyFeature
import openqwoutt.textprocessor.backend.modelscatalog.ModelsCatalogFeature
import openqwoutt.textprocessor.backend.promptstore.PromptStoreFeature
import openqwoutt.textprocessor.backend.repoindex.PromptRegistryServiceKey
import openqwoutt.textprocessor.backend.repoindex.RepoIndexFeature
import openqwoutt.textprocessor.backend.textprocessing.adapter.http.installTextProcessingRoutes
import openqwoutt.textprocessor.backend.textprocessing.application.LlmCompletionPort
import openqwoutt.textprocessor.backend.textprocessing.application.ProcessTextUseCase
import openqwoutt.textprocessor.backend.textprocessing.infrastructure.ChatCompletionsLlmAdapter
import openqwoutt.textprocessor.backend.textprocessing.infrastructure.NvidiaConnectionConfig
import openqwoutt.textprocessor.backend.textprocessing.infrastructure.OpenRouterConnectionConfig

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    embeddedServer(Netty, port = port, host = "0.0.0.0", module = Application::module).start(wait = true)
}

fun Application.module() {
    val json =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }

    RepoIndexFeature.install(this)
    PromptStoreFeature.install(this, json)

    val prometheusRegistry = createOptionalPrometheusRegistry()
    ModelsCatalogFeature.install(this, meterRegistry = prometheusRegistry)
    val llmMeterRegistry: MeterRegistry? = prometheusRegistry

    val llmRt: LlmUpstreamRuntime? = createLlmUpstreamRuntime(json, llmMeterRegistry)

    val promptProxyApiKey =
        System.getenv("PROMPT_PROXY_API_KEY")?.trim()?.takeIf { it.isNotEmpty() }
    PromptProxyFeature.install(
        application = this,
        json = json,
        llmHttpClient = llmRt?.httpClient,
        chatCompletionsUrl = llmRt?.chatCompletionsUrl,
        providerId = llmRt?.providerId,
        routingModel = resolvePromptProxyRoutingModel(llmRt),
        promptProxyApiKey = promptProxyApiKey,
        meterRegistry = llmMeterRegistry,
    )
    NvidiaProxyFeature.install(this)

    install(CallLogging)
    install(ContentNegotiation) {
        json(json)
    }
    installCorsFromEnvironment()

    val catalog = attributes.getOrNull(PromptRegistryServiceKey)?.let(::PromptRegistryCatalogAdapter)
    val processTextUseCase =
        ProcessTextUseCase(
            llm = llmRt?.llm,
            modelPromptCatalog = catalog,
            fallbackRoutingModelId = llmRt?.defaultRoutingModelId ?: "",
        )

    routing {
        get("/health") {
            call.respond(mapOf("status" to "ok"))
        }
    }

    installTextProcessingRoutes(processTextUseCase)

    installLlmOutboundRateLimitFromEnvironment()

    prometheusRegistry?.let { installPrometheusScrapeRoute(it) }
}

private const val OPENROUTER_CHAT_COMPLETIONS_URL: String = "https://openrouter.ai/api/v1/chat/completions"

private data class LlmUpstreamRuntime(
    val httpClient: HttpClient,
    val llm: LlmCompletionPort,
    val defaultRoutingModelId: String,
    val chatCompletionsUrl: String,
    val providerId: String,
)

private fun Application.createLlmUpstreamRuntime(json: Json, meterRegistry: MeterRegistry?): LlmUpstreamRuntime? {
    val provider = System.getenv("LLM_PROVIDER")?.trim()?.lowercase() ?: "openrouter"

    return when (provider) {
        "nvidia", "nim" ->
            runCatching { createNvidiaRuntime(json, meterRegistry) }
                .onFailure { log.warn("NVIDIA LLM disabled: ${it.message}", it) }
                .getOrNull()
        else ->
            runCatching { createOpenRouterRuntime(json, meterRegistry) }
                .onFailure { log.warn("OpenRouter LLM disabled: ${it.message}", it) }
                .getOrNull()
    }
}

private fun Application.createOpenRouterRuntime(json: Json, meterRegistry: MeterRegistry?): LlmUpstreamRuntime {
    val config = OpenRouterConnectionConfig.fromEnvironment()
    val client =
        createLlmHttpClient(json) {
            header(HttpHeaders.Authorization, "Bearer ${config.apiKey}")
            config.httpReferer?.let { header("HTTP-Referer", it) }
            header("X-Title", config.appTitle)
        }
    monitor.subscribe(ApplicationStopped) {
        client.close()
    }
    val llm =
        ChatCompletionsLlmAdapter(
            httpClient = client,
            chatCompletionsUrl = OPENROUTER_CHAT_COMPLETIONS_URL,
            providerId = "openrouter",
            json = json,
            meterRegistry = meterRegistry,
        )
    return LlmUpstreamRuntime(
        httpClient = client,
        llm = llm,
        defaultRoutingModelId = config.defaultRoutingModelId,
        chatCompletionsUrl = OPENROUTER_CHAT_COMPLETIONS_URL,
        providerId = "openrouter",
    )
}

private fun Application.createNvidiaRuntime(json: Json, meterRegistry: MeterRegistry?): LlmUpstreamRuntime {
    val config = NvidiaConnectionConfig.fromEnvironment()
    val client =
        createLlmHttpClient(json) {
            header(HttpHeaders.Authorization, "Bearer ${config.apiKey}")
        }
    monitor.subscribe(ApplicationStopped) {
        client.close()
    }
    val llm =
        ChatCompletionsLlmAdapter(
            httpClient = client,
            chatCompletionsUrl = config.chatCompletionsUrl,
            providerId = "nvidia",
            json = json,
            meterRegistry = meterRegistry,
        )
    return LlmUpstreamRuntime(
        httpClient = client,
        llm = llm,
        defaultRoutingModelId = config.defaultModelId,
        chatCompletionsUrl = config.chatCompletionsUrl,
        providerId = "nvidia",
    )
}

private fun resolvePromptProxyRoutingModel(runtime: LlmUpstreamRuntime?): String? {
    runtime ?: return null
    val fromEnv =
        System.getenv("LLM_PROMPT_PROXY_MODEL")?.trim()?.takeIf { it.isNotEmpty() }
            ?: System.getenv("OPENROUTER_PROMPT_PROXY_MODEL")?.trim()?.takeIf { it.isNotEmpty() }
    return fromEnv ?: runtime.defaultRoutingModelId.takeIf { it.isNotBlank() }
}
