package openqwoutt.textprocessor.backend.textprocessing.application

data class ProcessTextCommand(
    val text: String,
    val modeId: String,
    val explicitModelId: String?,
    val returnPromptDetails: Boolean,
)
