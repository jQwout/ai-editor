package openqwoutt.textstyler.data.settings

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object OpenRouterModelsCache {

    private val _models = MutableStateFlow<List<String>>(emptyList())
    val models: StateFlow<List<String>> = _models.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    suspend fun initialize(
        repository: OpenRouterModelsRepository,
        settingsRepository: SettingsRepository
    ) {
        _isLoading.value = true
        try {
            repository.fetchModels()
                .onSuccess { modelList ->
                    _models.value = modelList
                    val settings = settingsRepository.load()
                    if (modelList.isNotEmpty() && settings.aiModel.isBlank()) {
                        val first = modelList.first()
                        settingsRepository.save(settings.copy(aiModel = first))
                        Log.d(TAG, "Auto-selected first free model: $first")
                    }
                }
                .onFailure { throwable ->
                    Log.e(TAG, "Failed to load models on app start", throwable)
                }
        } finally {
            _isLoading.value = false
        }
    }

    private const val TAG = "OpenRouterModelsCache"
}
