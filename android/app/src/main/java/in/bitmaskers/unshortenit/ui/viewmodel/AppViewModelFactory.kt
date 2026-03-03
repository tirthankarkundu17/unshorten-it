package `in`.bitmaskers.unshortenit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import `in`.bitmaskers.unshortenit.data.repository.HistoryRepository
import `in`.bitmaskers.unshortenit.data.repository.UnshortenRepository

class AppViewModelFactory(
    private val historyRepository: HistoryRepository,
    private val unshortenRepository: UnshortenRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            return DashboardViewModel(historyRepository) as T
        }
        if (modelClass.isAssignableFrom(InterceptorViewModel::class.java)) {
            return InterceptorViewModel(unshortenRepository, historyRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
