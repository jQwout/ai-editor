package openqwoutt.textprocessor.backend.promptproxy

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post

fun Route.promptProxyRoutes(service: PromptProxyService) {
    post("/api/prompt/proxy") {
        handlePromptProxy(call, service)
    }
}

suspend fun handlePromptProxy(call: ApplicationCall, service: PromptProxyService) {
    val req = runCatching { call.receive<PromptProxyRequest>() }.getOrElse { err ->
        call.respond(
            HttpStatusCode.BadRequest,
            PromptProxyErrorEnvelope(
                PromptProxyErrorDetail(
                    message =
                        "Invalid JSON body: ${err.message ?: err::class.simpleName}",
                    providerRaw = err.stackTraceToString(),
                ),
            ),
        )
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
