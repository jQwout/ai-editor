package openqwoutt.miniapp.textstyler.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import openqwoutt.miniapp.textstyler.data.repository.InteractionRepository
import openqwoutt.miniapp.textstyler.domain.model.Interaction

/**
 * ViewModel for History screen.
 */
class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = InteractionRepository(application)

    private val _state = MutableStateFlow(HistoryState())
    val state: StateFlow<HistoryState> = _state.asStateFlow()

    init {
        loadHistory()
    }

    fun loadHistory() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val items = repository.getAll(HISTORY_LIMIT)
                _state.update { it.copy(items = items, isLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            try {
                repository.delete(id)
                loadHistory()  // Reload after delete
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun deleteAll() {
        viewModelScope.launch {
            try {
                repository.deleteAll()
                _state.update { it.copy(items = emptyList()) }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    companion object {
        private const val HISTORY_LIMIT = 100
    }
}

/**
 * State for History screen.
 */
data class HistoryState(
    val items: List<Interaction> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * Actions for History screen.
 */
sealed class HistoryAction {
    data class Expand(val id: Long) : HistoryAction()
    data class Collapse(val id: Long) : HistoryAction()
    data class Delete(val id: Long) : HistoryAction()
    data object DeleteAll : HistoryAction()
    data object Retry : HistoryAction()
    data object Refresh : HistoryAction()
}