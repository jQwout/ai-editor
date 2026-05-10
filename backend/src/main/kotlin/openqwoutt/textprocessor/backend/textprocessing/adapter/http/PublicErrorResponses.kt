package openqwoutt.textprocessor.backend.textprocessing.adapter.http

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import openqwoutt.textprocessor.backend.textprocessing.application.ProcessTextOutcome

/**
 * Maps domain errors to HTTP JSON. Upstream payloads and diagnostics are passed through unchanged
 * (`providerRaw`, `detail`, parsed `providerBody`).
 */
fun buildProcessTextErrorResponse(outcome: ProcessTextOutcome.Err): ErrorResponseDto =
    ErrorResponseDto(
        error = outcome.message,
        providerId = outcome.providerId,
        httpStatus = outcome.httpStatus,
        providerRaw = outcome.providerRaw,
        providerBody = providerJsonOrNull(outcome.providerRaw),
        detail = outcome.detail,
    )

private val textProcessingErrorJson = Json { ignoreUnknownKeys = true }

private fun providerJsonOrNull(raw: String?): JsonElement? {
    if (raw.isNullOrBlank()) return null
    return runCatching {
        textProcessingErrorJson.decodeFromString(JsonElement.serializer(), raw)
    }.getOrNull()
}
