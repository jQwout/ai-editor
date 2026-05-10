package openqwoutt.textprocessor.backend.textprocessing.adapter.http

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.log
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import openqwoutt.textprocessor.backend.textprocessing.application.ProcessTextCommand
import openqwoutt.textprocessor.backend.textprocessing.application.ProcessTextErrorKind
import openqwoutt.textprocessor.backend.textprocessing.application.ProcessTextOutcome
import openqwoutt.textprocessor.backend.textprocessing.application.ProcessTextUseCase

fun Application.installTextProcessingRoutes(useCase: ProcessTextUseCase) {
    routing {
        textProcessingRoutes(useCase)
    }
}

fun Routing.textProcessingRoutes(useCase: ProcessTextUseCase) {
    post("/api/text/process") {
        val request =
            try {
                call.receive<ProcessTextRequestDto>()
            } catch (_: BadRequestException) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponseDto(error = "Invalid JSON or request body could not be parsed."),
                )
                return@post
            } catch (_: kotlinx.serialization.SerializationException) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponseDto(error = "Invalid JSON or request body could not be parsed."),
                )
                return@post
            }

        val outcome =
            useCase.execute(
                ProcessTextCommand(
                    text = request.text,
                    modeId = request.mode,
                    explicitModelId = request.model,
                    returnPromptDetails = request.returnPrompt == true,
                ),
            )
        when (outcome) {
            is ProcessTextOutcome.Ok ->
                call.respond(
                    ProcessTextResponseDto(
                        result = outcome.value.result,
                        model = outcome.value.routingModelId,
                        mode = outcome.value.modeId,
                        promptText = outcome.value.promptText,
                        temperature = outcome.value.temperature,
                    ),
                )
            is ProcessTextOutcome.Err -> {
                val status =
                    when (outcome.kind) {
                        ProcessTextErrorKind.OPENROUTER_DISABLED -> HttpStatusCode.ServiceUnavailable
                        ProcessTextErrorKind.BAD_REQUEST -> HttpStatusCode.BadRequest
                        ProcessTextErrorKind.REGISTRY_REQUIRED -> HttpStatusCode.ServiceUnavailable
                        ProcessTextErrorKind.UNKNOWN_MODE -> HttpStatusCode.BadRequest
                        ProcessTextErrorKind.MODEL_PROMPT_NOT_FOUND -> HttpStatusCode.BadRequest
                        ProcessTextErrorKind.AI_BACKEND_FAILED -> HttpStatusCode.BadGateway
                    }
                if (outcome.kind == ProcessTextErrorKind.AI_BACKEND_FAILED) {
                    call.application.environment.log.warn("LLM upstream request failed", outcome.cause)
                }
                call.respond(status, buildProcessTextErrorResponse(outcome))
            }
        }
    }
}
