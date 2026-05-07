package openqwoutt.miniapp.textstyler.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import openqwoutt.miniapp.textstyler.data.prompts.PromptCategory
import openqwoutt.miniapp.textstyler.data.prompts.PromptRepository
import openqwoutt.miniapp.textstyler.data.prompts.PromptTemplate
import openqwoutt.miniapp.textstyler.domain.StyleMode
import openqwoutt.miniapp.textstyler.domain.TextProcessorUseCase
import openqwoutt.miniapp.textstyler.domain.TextStylerResult
import openqwoutt.textstyler.data.settings.AppSettings
import openqwoutt.textstyler.data.settings.OpenRouterModelsCache
import openqwoutt.textstyler.data.settings.SettingsRepository
import openqwoutt.miniapp.textstyler.data.repository.InteractionRepository

/**
 * One-shot events from ViewModel to UI.
 */
sealed class MiniAppEvent {
    data class ResultReady(val result: String) : MiniAppEvent()
    data object NavigateBack : MiniAppEvent()
}

data class TextStylerState(
    val inputText: String = "",
    val initialInputText: String? = null,
    val selectedMode: StyleMode = StyleMode.STYLE,
    val selectedTemplate: PromptTemplate? = null,
    val availableTemplates: List<PromptTemplate> = emptyList(),
    val availableCategories: List<PromptCategory> = emptyList(),
    val selectedCategory: String? = null,
    val searchQuery: String = "",
    val showTemplates: Boolean = false,
    val showHistory: Boolean = false,
    val showModePicker: Boolean = false,
    val result: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isTextTruncated: Boolean = false,
    val showSettings: Boolean = false,
    val settings: AppSettings = AppSettings(),
    val closeBehavior: CloseBehavior = CloseBehavior.NavigateBack,
    val availableModels: List<String> = emptyList(),
    val isLoadingModels: Boolean = false
)

enum class CloseBehavior {
    NavigateBack,
    FinishActivity,
    FinishWithResult,
    CopyToClipboard
}

sealed class TextStylerAction {
    data class SetInputText(val text: String) : TextStylerAction()
    data class SelectMode(val mode: StyleMode) : TextStylerAction()
    data object ProcessText : TextStylerAction()
    data object ClearResult : TextStylerAction()
    data object ClearError : TextStylerAction()
    data object ToggleSettings : TextStylerAction()
    data class SaveSettings(val settings: AppSettings) : TextStylerAction()
    data class ReloadUseCaseWithSettings(val settings: AppSettings) : TextStylerAction()
    data class SelectTemplate(val template: PromptTemplate?) : TextStylerAction()
    data object ShowTemplates : TextStylerAction()
    data object HideTemplates : TextStylerAction()
    data object ShowHistory : TextStylerAction()
    data object HideHistory : TextStylerAction()
    data object ToggleModePicker : TextStylerAction()
    data object ShowModePicker : TextStylerAction()
    data object HideModePicker : TextStylerAction()
    data class SelectCategory(val category: String?) : TextStylerAction()
    data class SearchTemplates(val query: String) : TextStylerAction()
    data class SetCloseBehavior(val behavior: CloseBehavior) : TextStylerAction()
    data class SetOnResultReady(val callback: (String) -> Unit, val onClose: () -> Unit = {}) : TextStylerAction()
}

class TextStylerViewModel(
    private var textProcessorUseCase: TextProcessorUseCase,
    private val settingsRepository: SettingsRepository,
    private val promptRepository: PromptRepository,
    private val interactionRepository: InteractionRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TextStylerState())
    val state: StateFlow<TextStylerState> = _state.asStateFlow()
    
    // SharedFlow for one-shot events (avoid storing callbacks in ViewModel)
    private val _events = MutableSharedFlow<MiniAppEvent>()
    val events: SharedFlow<MiniAppEvent> = _events.asSharedFlow()

    init {
        val saved = settingsRepository.load()
        val templates = promptRepository.getTemplates()
        val categories = promptRepository.getCategories()
        _state.update { it.copy(settings = saved, availableTemplates = templates, availableCategories = categories) }

        // Sync available models from OpenRouterModelsCache
        _state.update { it.copy(availableModels = OpenRouterModelsCache.models.value, isLoadingModels = OpenRouterModelsCache.isLoading.value) }

        // Apply initial input text if provided
        _state.value.initialInputText?.let { initialText ->
            _state.update { it.copy(inputText = initialText) }
        }
    }

    fun setInitialInputText(text: String) {
        _state.update { it.copy(initialInputText = text, inputText = text) }
    }

    fun handle(action: TextStylerAction) {
        when (action) {
            is TextStylerAction.SetInputText -> {
                _state.update { it.copy(inputText = action.text, error = null) }
            }
            is TextStylerAction.SelectMode -> {
                _state.update { it.copy(selectedMode = action.mode, error = null) }
            }
            is TextStylerAction.ProcessText -> processText()
            is TextStylerAction.ClearResult -> {
                _state.update { it.copy(result = null) }
            }
            is TextStylerAction.ClearError -> {
                _state.update { it.copy(error = null) }
            }
            is TextStylerAction.ToggleSettings -> {
                _state.update { it.copy(showSettings = !it.showSettings) }
            }
            is TextStylerAction.SaveSettings -> {
                settingsRepository.save(action.settings)
                _state.update {
                    it.copy(settings = action.settings, showSettings = false, error = null)
                }
                // Reload the use case with new settings
                textProcessorUseCase = TextProcessorUseCase(settings = action.settings)
            }
            is TextStylerAction.ReloadUseCaseWithSettings -> {
                textProcessorUseCase = TextProcessorUseCase(settings = action.settings)
            }
            is TextStylerAction.SelectTemplate -> {
                _state.update { it.copy(selectedTemplate = action.template) }
            }
            is TextStylerAction.ShowTemplates -> {
                _state.update { it.copy(showTemplates = true) }
            }
            is TextStylerAction.HideTemplates -> {
                _state.update { it.copy(showTemplates = false) }
            }
            is TextStylerAction.ShowHistory -> {
                _state.update { it.copy(showHistory = true) }
            }
            is TextStylerAction.HideHistory -> {
                _state.update { it.copy(showHistory = false) }
            }
            is TextStylerAction.ToggleModePicker -> {
                _state.update { it.copy(showModePicker = !it.showModePicker) }
            }
            is TextStylerAction.ShowModePicker -> {
                _state.update { it.copy(showModePicker = true) }
            }
            is TextStylerAction.HideModePicker -> {
                _state.update { it.copy(showModePicker = false) }
            }
            is TextStylerAction.SelectCategory -> {
                _state.update { it.copy(selectedCategory = action.category) }
            }
            is TextStylerAction.SearchTemplates -> {
                val query = action.query
                val results = if (query.isBlank()) {
                    _state.value.availableTemplates
                } else {
                    promptRepository.search(query)
                }
                _state.update { it.copy(searchQuery = query, availableTemplates = results) }
            }
            is TextStylerAction.SetCloseBehavior -> {
                _state.update { it.copy(closeBehavior = action.behavior) }
            }
            is TextStylerAction.SetOnResultReady -> {
                // We don't store callbacks in ViewModel anymore
                // UI collects events from SharedFlow
            }
        }
    }

    private fun processText() {
        val currentState = _state.value
        if (currentState.inputText.isBlank()) {
            _state.update { it.copy(error = "Add text or paste screenshot OCR first") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, result = null) }

            when (
                val result = textProcessorUseCase.processText(
                    inputText = currentState.inputText,
                    mode = currentState.selectedMode,
                    template = currentState.selectedTemplate
                )
            ) {
                is TextStylerResult.Success -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            result = result.result,
                            isTextTruncated = currentState.inputText.length > 3000
                        )
                    }
                    // Save to history (opt-out)
                    if (currentState.settings.saveHistory) {
                        interactionRepository.save(
                            inputText = currentState.inputText,
                            outputText = result.result,
                            mode = currentState.selectedMode.id,
                            status = openqwoutt.miniapp.textstyler.domain.model.InteractionStatus.SUCCESS
                        )
                    }
                    // Emit event for UI to handle callbacks
                    when (_state.value.closeBehavior) {
                        CloseBehavior.FinishWithResult -> {
                            _events.emit(MiniAppEvent.ResultReady(result.result))
                            _events.emit(MiniAppEvent.NavigateBack)
                        }
                        CloseBehavior.CopyToClipboard -> {
                            _events.emit(MiniAppEvent.ResultReady(result.result))
                        }
                        CloseBehavior.NavigateBack, CloseBehavior.FinishActivity -> {
                            // Do nothing extra, just show result
                        }
                    }
                }
                TextStylerResult.EmptyInput -> {
                    _state.update {
                        it.copy(isLoading = false, error = "Add text or paste screenshot OCR first")
                    }
                }
                TextStylerResult.OrchestratorFailed -> {
                    val errorMsg = "Could not process this text. Try again."
                    _state.update {
                        it.copy(isLoading = false, error = errorMsg)
                    }
                    // Save error to history
                    interactionRepository.save(
                        inputText = currentState.inputText,
                        outputText = null,
                        mode = currentState.selectedMode.id,
                        status = openqwoutt.miniapp.textstyler.domain.model.InteractionStatus.ERROR,
                        errorMessage = errorMsg
                    )
                }
            }
        }
    }
}
