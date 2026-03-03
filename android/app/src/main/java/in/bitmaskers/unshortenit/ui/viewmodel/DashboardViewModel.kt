package `in`.bitmaskers.unshortenit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.bitmaskers.unshortenit.data.model.HistoryItem
import `in`.bitmaskers.unshortenit.data.repository.HistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

import `in`.bitmaskers.unshortenit.data.repository.UnshortenRepository

class DashboardViewModel(
    private val historyRepository: HistoryRepository,
    private val unshortenRepository: UnshortenRepository
) : ViewModel() {

    private val _isUnshortening = MutableStateFlow(false)
    val isUnshortening: StateFlow<Boolean> = _isUnshortening
    
    private val _unshortenError = MutableStateFlow<String?>(null)
    val unshortenError: StateFlow<String?> = _unshortenError

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

    fun unshortenUrl(url: String) {
        if (url.isBlank()) return
        
        viewModelScope.launch {
            _isUnshortening.value = true
            _unshortenError.value = null
            val existingItem = historyRepository.getHistoryByUrl(url)
            if (existingItem != null) {
                // Return early with the cached version but trigger a reload so it hops back to top
                // Optionally update its timestamp in DB so it actually jumps to top (deleting old + inserting new)
                historyRepository.insertHistory(
                    originalUrl = url,
                    finalUrl = existingItem.finalUrl,
                    responseTime = existingItem.responseTime,
                    redirectChain = null // Keep it visually simple or deserialize it based on how we handle it
                )
                loadHistory(isRefresh = true)
                _isUnshortening.value = false
                return@launch
            }

            val result = unshortenRepository.unshortenUrl(url)
            result.onSuccess { response ->
                try {
                    historyRepository.insertHistory(
                        originalUrl = url,
                        finalUrl = response.finalUrl,
                        responseTime = response.responseTimeMs,
                        redirectChain = response.redirectChain
                    )
                    loadHistory(isRefresh = true)
                } catch (e: Exception) {
                    _unshortenError.value = "Failed to save to history"
                }
            }.onFailure { e ->
                _unshortenError.value = e.message ?: "Failed to unshorten URL"
            }
            
            _isUnshortening.value = false
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            try {
                historyRepository.clearHistory()
                loadHistory(isRefresh = true)
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Failed to clear history")
            }
        }
    }
}
