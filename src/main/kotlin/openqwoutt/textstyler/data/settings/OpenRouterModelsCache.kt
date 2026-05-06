package openqwoutt.textstyler.data.settings

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object OpenRouterModelsCache {

    private val _models = MutableStateFlow<List<String>>(emptyList())
    val models: StateFlow<List<String>> = _models.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    suspend fun initialize(context: Context) {
        _isLoading.value = true
        val repository = OpenRouterModelsRepository()
        val settingsRepository = SettingsRepository(context)

        repository.fetchModels()
            .onSuccess { modelList ->
                _models.value = modelList
                val settings = settingsRepository.load()
                if (modelList.isNotEmpty() && (settings.aiModel.isEmpty() || settings.aiModel !in modelList)) {
                    val first = modelList.first()
                    settingsRepository.save(settings.copy(aiModel = first))
                    Log.d(TAG, "Auto-selected first free model: $first")
                }
            }
            .onFailure { throwable ->
                Log.e(TAG, "Failed to load models on app start", throwable)
            }

        _isLoading.value = false
    }

    private const val TAG = "OpenRouterModelsCache"
}
