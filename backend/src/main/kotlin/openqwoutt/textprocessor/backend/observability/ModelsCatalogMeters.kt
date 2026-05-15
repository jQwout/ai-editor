package openqwoutt.textprocessor.backend.observability

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import java.util.concurrent.TimeUnit

fun MeterRegistry?.recordModelsCatalogSync(
    durationMs: Long,
    success: Boolean,
    applied: Int,
    disabled: Int,
) {
    val registry = this ?: return
    Timer.builder("models_catalog.sync")
        .tag("status", if (success) "success" else "failure")
        .register(registry)
        .record(durationMs.coerceAtLeast(0L), TimeUnit.MILLISECONDS)
    registry.counter(
        "models_catalog.sync.applied",
        "status",
        if (success) "success" else "failure",
    ).increment(applied.coerceAtLeast(0).toDouble())
    registry.counter(
        "models_catalog.sync.disabled",
        "status",
        if (success) "success" else "failure",
    ).increment(disabled.coerceAtLeast(0).toDouble())
}
