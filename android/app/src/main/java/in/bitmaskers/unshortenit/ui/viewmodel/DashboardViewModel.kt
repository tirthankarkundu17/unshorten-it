package `in`.bitmaskers.unshortenit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.bitmaskers.unshortenit.data.model.HistoryItem
import `in`.bitmaskers.unshortenit.data.repository.HistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val historyRepository: HistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<HistoryItem>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<HistoryItem>>> = _uiState

    init {
        loadHistory()
    }

    fun loadHistory(isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (!isRefresh) {
                _uiState.value = UiState.Loading
            }
            try {
                val historyItems = historyRepository.getAllHistory()
                _uiState.value = UiState.Success(historyItems)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to load history")
            }
        }
    }
}
