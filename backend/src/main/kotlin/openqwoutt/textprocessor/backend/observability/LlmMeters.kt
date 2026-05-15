package openqwoutt.textprocessor.backend.observability

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import java.util.concurrent.TimeUnit

fun MeterRegistry?.recordLlmRequest(
    providerId: String,
    operation: String,
    durationMs: Long,
    success: Boolean,
) {
    val registry = this ?: return
    Timer.builder("llm.request")
        .tag("provider", providerId)
        .tag("operation", operation)
        .tag("status", if (success) "success" else "failure")
        .register(registry)
        .record(durationMs.coerceAtLeast(0L), TimeUnit.MILLISECONDS)
}
