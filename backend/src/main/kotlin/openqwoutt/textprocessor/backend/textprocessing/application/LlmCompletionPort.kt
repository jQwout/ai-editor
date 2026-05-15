package openqwoutt.textprocessor.backend.textprocessing.application

fun interface LlmCompletionPort {
    suspend fun complete(
        userText: String,
        taskPrompt: String,
        temperature: Double,
        routingModelId: String,
    ): String
}
