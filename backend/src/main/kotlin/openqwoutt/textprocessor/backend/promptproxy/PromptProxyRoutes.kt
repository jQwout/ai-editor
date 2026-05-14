package openqwoutt.textprocessor.backend.promptproxy

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondTextWriter
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.collect
import kotlinx.serialization.json.Json
import kotlin.coroutines.coroutineContext
import openqwoutt.textprocessor.backend.security.secureEqualsUtf8Strings

private val promptProxySseJson =
    Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

fun Route.promptProxyRoutes(service: PromptProxyService, promptProxyApiKey: String) {
    post("/api/prompt/proxy") {
        handlePromptProxy(call, service, promptProxyApiKey)
    }
}

suspend fun handlePromptProxy(call: ApplicationCall, service: PromptProxyService, promptProxyApiKey: String) {
    val authHeader = call.request.headers[HttpHeaders.Authorization].orEmpty()
    val bearer = authHeader.removePrefix("Bearer ").trim()
    if (!secureEqualsUtf8Strings(bearer, promptProxyApiKey)) {
        call.respond(
            HttpStatusCode.Unauthorized,
            PromptProxyErrorEnvelope(
                error = PromptProxyErrorDetail(message = "Unauthorized."),
            ),
        )
        return
    }

    val req = runCatching { call.receive<PromptProxyRequest>() }.getOrElse { err ->
        call.application.environment.log.warn("Prompt proxy: invalid JSON body (${err.message})", err)
        call.respond(
            HttpStatusCode.BadRequest,
            PromptProxyErrorEnvelope(
                PromptProxyErrorDetail(
                    message =
                        "Invalid JSON body: ${err.message ?: err::class.simpleName}",
                ),
            ),
        )
        return
    }

    if (req.stream) {
        when (val prep = service.streamPrepare(req)) {
            is PromptProxyStreamPrepare.Validation ->
                call.respond(HttpStatusCode.BadRequest, PromptProxyErrorEnvelope(error = prep.detail))

            is PromptProxyStreamPrepare.Frames ->
                call.respondTextWriter(contentType = ContentType.Text.EventStream) {
                    var failed = false
                    prep.flow.collect { frame ->
                        coroutineContext.ensureActive()
                        when (frame) {
                            is ProxyStreamFrame.Delta -> {
                                val payload =
                                    promptProxySseJson.encodeToString(
                                        PromptProxySseChunkDto.serializer(),
                                        PromptProxySseChunkDto(frame.text),
                                    )
                                append("data: ")
                                append(payload)
                                append("\n\n")
                            }
                            is ProxyStreamFrame.Failed -> {
                                failed = true
                                val payload =
                                    promptProxySseJson.encodeToString(
                                        PromptProxyErrorDetail.serializer(),
                                        frame.detail,
                                    )
                                append("event: error\n")
                                append("data: ")
                                append(payload)
                                append("\n\n")
                            }
                        }
                    }
                    if (!failed) {
                        append("event: done\n")
                        append("data: {}\n\n")
                    }
                }
        }
        return
    }

    when (val outcome = service.run(req)) {
        is PromptProxyRunOutcome.Ok ->
            call.respond(HttpStatusCode.OK, PromptProxySuccessResponse(result = outcome.text))

        is PromptProxyRunOutcome.Validation ->
            call.respond(HttpStatusCode.BadRequest, PromptProxyErrorEnvelope(error = outcome.detail))

        is PromptProxyRunOutcome.Provider ->
            call.respond(
                HttpStatusCode.BadGateway,
                PromptProxyErrorEnvelope(error = outcome.detail),
            )
    }
}
