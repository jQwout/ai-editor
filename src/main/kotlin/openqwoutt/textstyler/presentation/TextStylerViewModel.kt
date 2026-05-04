package openqwoutt.miniapp.textstyler.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import openqwoutt.miniapp.textstyler.domain.StyleMode
import openqwoutt.miniapp.textstyler.domain.TextProcessorUseCase
import openqwoutt.miniapp.textstyler.domain.TextStylerResult
import openqwoutt.textstyler.data.settings.AppSettings
import openqwoutt.textstyler.data.settings.SettingsRepository

data class TextStylerState(
    val inputText: String = "",
    val selectedMode: StyleMode = StyleMode.STYLE,
    val result: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isTextTruncated: Boolean = false,
    val showSettings: Boolean = false,
    val settings: AppSettings = AppSettings()
)

sealed class TextStylerAction {
    data class SetInputText(val text: String) : TextStylerAction()
    data class SelectMode(val mode: StyleMode) : TextStylerAction()
    data object ProcessText : TextStylerAction()
    data object ClearResult : TextStylerAction()
    data object ClearError : TextStylerAction()
    data object ToggleSettings : TextStylerAction()
    data class SaveSettings(val settings: AppSettings) : TextStylerAction()
}

class TextStylerViewModel(
    private val textProcessorUseCase: TextProcessorUseCase,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TextStylerState())
    val state: StateFlow<TextStylerState> = _state.asStateFlow()

    init {
        val saved = settingsRepository.load()
        _state.update { it.copy(settings = saved) }
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
                    mode = currentState.selectedMode
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
                }
                TextStylerResult.EmptyInput -> {
                    _state.update {
                        it.copy(isLoading = false, error = "Add text or paste screenshot OCR first")
                    }
                }
                TextStylerResult.OrchestratorFailed -> {
                    _state.update {
                        it.copy(isLoading = false, error = "Could not process this text. Try again.")
                    }
                }
            }
        }
    }
}
