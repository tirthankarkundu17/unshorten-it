package `in`.bitmaskers.unshortenit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.bitmaskers.unshortenit.data.api.UnshortenResponse
import `in`.bitmaskers.unshortenit.data.repository.HistoryRepository
import `in`.bitmaskers.unshortenit.data.repository.UnshortenRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class InterceptorViewModel(
    private val unshortenRepository: UnshortenRepository,
    private val historyRepository: HistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<Map<String, Result<UnshortenResponse>>>>(UiState.Loading)
    val uiState: StateFlow<UiState<Map<String, Result<UnshortenResponse>>>> = _uiState

    fun processUrls(urls: List<String>) {
        if (urls.isEmpty()) {
            _uiState.value = UiState.Success(emptyMap())
            return
        }

        viewModelScope.launch {
            _uiState.value = UiState.Loading
            
            val results = mutableMapOf<String, Result<UnshortenResponse>>()
            urls.forEach { url ->
                val result = unshortenRepository.unshortenUrl(url)
                
                result.onSuccess { response ->
                    try {
                        historyRepository.insertHistory(
                            originalUrl = url,
                            finalUrl = response.finalUrl,
                            responseTime = response.responseTimeMs,
                            redirectChain = response.redirectChain
                        )
                    } catch (e: Exception) {
                        // ignore DB insertion error if happens
                    }
                }
                
                results[url] = result
            }
            
            _uiState.value = UiState.Success(results)
        }
    }
}
