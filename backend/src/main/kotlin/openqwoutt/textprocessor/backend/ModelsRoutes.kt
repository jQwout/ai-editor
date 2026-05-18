package openqwoutt.textprocessor.backend

import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.serialization.Serializable

/**
 * Models list endpoint for frontend.
 * Returns available AI models from backend.
 */
fun Route.modelsRoutes() {
    get("/api/models") {
        val models = listOf(
            ModelInfo(
                id = "meta/llama-3.1-70b-instruct",
                displayName = "Llama 3.1 70B Instruct",
                provider = "nvidia"
            ),
            ModelInfo(
                id = "qwen/qwen3-coder-480b-a35b-instruct",
                displayName = "Qwen3 Coder 480B",
                provider = "nvidia"
            )
        )
        call.respond(ModelsResponse(models = models))
    }
}

@Serializable
data class ModelsResponse(
    val models: List<ModelInfo>
)

@Serializable
data class ModelInfo(
    val id: String,
    val displayName: String,
    val provider: String
)