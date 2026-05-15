package openqwoutt.textprocessor.backend.repoindex

import io.ktor.server.application.Application
import io.ktor.server.application.log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.hours

object PublicRepoScheduler {

    fun start(
        app: Application,
        refresher: PublicRepoRefresher,
    ): Job? {
        val enabled = System.getenv("PUBLIC_PROMPT_AUTO_REFRESH")?.equals("true", ignoreCase = true) == true
        if (!enabled) {
            app.log.info("Public repo auto refresh disabled (set PUBLIC_PROMPT_AUTO_REFRESH=true to enable).")
            return null
        }

        val intervalHours = System.getenv("PUBLIC_PROMPT_REFRESH_INTERVAL_HOURS")?.toLongOrNull() ?: 24L
        val interval = intervalHours.coerceAtLeast(1).hours

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        return scope.launch {
            app.log.info("Public repo auto refresh started (interval=${interval.inWholeHours}h).")
            while (isActive) {
                try {
                    refresher.refreshOnce(app)
                } catch (ce: CancellationException) {
                    throw ce
                } catch (t: Throwable) {
                    app.log.warn("Public repo auto refresh tick failed", t)
                }
                delay(interval)
            }
        }
    }
}

